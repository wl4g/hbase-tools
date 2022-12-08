package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomUtils.nextDouble;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;
import com.wl4g.infra.common.lang.DateUtils2;
import com.wl4g.tools.hbase.phoenix.config.ToolsProperties.RunnerProvider;
import com.wl4g.tools.hbase.phoenix.exception.IllegalFakeValueToolsException;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * 累计列 Fake 数据处理器. 即, 列的数值是累计递增的, 如: 电表电度读数时序数据.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public class MonotoneIncreasePhoenixTableFaker extends PhoenixTableFaker {

    @Override
    protected RunnerProvider provider() {
        return RunnerProvider.MONOTONE_INCREASE_FAKER;
    }

    @Override
    protected Runnable newProcessTask(String sampleStartRowKey, String sampleEndRowKey) {
        return new ProcessTask(sampleStartRowKey, sampleEndRowKey);
    }

    class ProcessTask implements Runnable {
        private final String sampleStartRowKey;
        private final String sampleEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);
        private final Map<String, AtomicDouble> lastMaxFakeValues = new HashMap<>();
        private Map<String, Double> upperLimitValues;
        private Map<String, Double> lowerLimitValues;

        public ProcessTask(String sampleStartRowKey, String sampleEndRowKey) {
            this.sampleStartRowKey = sampleStartRowKey;
            this.sampleEndRowKey = sampleEndRowKey;
        }

        @Override
        public void run() {
            try {
                final long begin = currentTimeMillis();
                List<Map<String, Object>> sampleRecords = fetchSampleRecords(sampleStartRowKey, sampleEndRowKey);
                totalOfAll.addAndGet(sampleRecords.size());

                // value 随着 rowKey 中时间是单调递增的, 无法使用 parallelStream
                for (Map<String, Object> sampleRecord : safeList(sampleRecords)) {
                    Map<String, Object> newRecord = new HashMap<>();
                    try {
                        // Gets averages based on before history samples data.
                        final long begin1 = currentTimeMillis();
                        Map<String, Double> incrementValues = getBeforeAverageIncrementValues(sampleRecord);
                        log.info("Avg increment values: {}, sampleRecord: {}, cost: {}ms", incrementValues, sampleRecord,
                                (currentTimeMillis() - begin1));

                        // Gets upper limit based on after actual data.
                        if (isNull(upperLimitValues)) {
                            upperLimitValues = getUpperLimitFakeValues(sampleRecord);
                            log.info("Upper limit values: {}, sampleRecord: {}", sampleRecord, upperLimitValues);
                        }
                        // Gets lower limit based on before actual data.
                        if (isNull(lowerLimitValues)) {
                            lowerLimitValues = getLowerLimitFakeValues(sampleRecord);
                            log.info("Lower limit values: {}, sampleRecord: {}", sampleRecord, lowerLimitValues);
                        }

                        // Generate random fake new record.
                        for (Map.Entry<String, Object> e : safeMap(sampleRecord).entrySet()) {
                            String columnName = e.getKey();
                            Object value = e.getValue();
                            Double incrementCount = incrementValues.get(AVG_COUNT_KEY);
                            Double incrementValue = incrementValues.get(columnName);
                            Double upperLimitValue = upperLimitValues.get(columnName);
                            Double lowerLimitValue = lowerLimitValues.get(columnName);

                            if (eqIgnCase(columnName, config.getRowKey().getName())) {
                                newRecord.put(columnName, generateFakeRowKey((String) value));
                            } else {
                                if (nonNull(incrementValue)) {
                                    Object fakeValue = generateFakeValue(columnName, sampleRecord, incrementCount, incrementValue,
                                            upperLimitValue, lowerLimitValue, value);
                                    newRecord.put(columnName, fakeValue);
                                } else if (!config.getFaker().getColumnNames().contains(columnName)) {
                                    newRecord.put(columnName, value);
                                } else {
                                    log.warn("TODO 列 '{}' 属于累计值，无法直接使用历史值作为 Fake 值, 必须知道增量才有意义, 或将此列从累计列移除.", columnName);
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (config.isErrorContinue() && !(e instanceof IllegalFakeValueToolsException)) {
                            log.warn(format("Unable not generate fake row for %s.", sampleRecord), e);
                        } else {
                            throw e;
                        }
                    }
                    try {
                        writeToHTable(newRecord);
                        completed.incrementAndGet();
                    } catch (Exception e) {
                        if (config.isErrorContinue() && !(e instanceof IllegalFakeValueToolsException)) {
                            log.warn(format("Unable write to htable for %s.", sampleRecord), e);
                        } else {
                            throw e;
                        }
                    }
                }
                log.info("Processed completed of {}/{}/{}/{}, sampleStartRowKey: {}, sampleEndRowKey: {}, cost: {}ms",
                        completed.get(), sampleRecords.size(), completedOfAll.get(), totalOfAll.get(), sampleStartRowKey,
                        sampleEndRowKey, (currentTimeMillis() - begin));
            } catch (Exception e) {
                if (config.isErrorContinue()) {
                    log.error(format("Unable to process and write to htable of sampleStartRowKey: %s, sampleEndRowKey: %s",
                            sampleStartRowKey, sampleEndRowKey), e);
                } else {
                    throw new IllegalStateException(e);
                }
            }
        }

        /**
         * 获取生成的累计值的上限 (fakeEndDate 之后的最小值). </br>
         * for example:
         * 
         * <code>
         * select rount(min(to_number("activePower")),4) as activePower,rount(min(to_number("reactivePower")),4) as reactivePower
         * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,202210220834' and "ROW"<='11111277,ELE_P,134,01,202210240834' limit 10;
         * </code>
         * 
         * @throws ParseException
         */
        private Map<String, Double> getUpperLimitFakeValues(Map<String, Object> sampleRecord) throws ParseException {
            final String sampleRowKey = (String) sampleRecord.get(config.getRowKey().getName());
            final Map<String, String> sampleRowKeyParts = config.getRowKey().from(sampleRowKey);

            // 从 fakeEndDate 开始向后取任意时间点作为结束时间(这里硬编码为1个周期),
            final Date upperLimitStartDate = getOffsetDate(getFakeEndDate(), config.getFaker().getSampleLastDatePattern(), 0);
            final Date upperLimitEndDate = getOffsetDate(getFakeEndDate(), config.getFaker().getSampleLastDatePattern(),
                    config.getFaker().getSampleLastDateAmount() * 1);
            final String upperLimitStartRowKey = generateRowKey(sampleRowKeyParts, upperLimitStartDate);
            final String upperLimitEndRowKey = generateRowKey(sampleRowKeyParts, upperLimitEndDate);

            StringBuilder columns = new StringBuilder();
            for (String columnName : safeList(config.getFaker().getColumnNames())) {
                columns.append("round(min(to_number(\"");
                columns.append(columnName);
                columns.append("\")),4) as ");
                columns.append(columnName);
                columns.append(",");
            }
            columns.delete(columns.length() - 1, columns.length());

            String queryLimitSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                    config.getTableNamespace(), config.getTableName(), upperLimitStartRowKey, upperLimitEndRowKey);
            log.debug("Query upperLimit sql: {}", queryLimitSql);

            List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryLimitSql));
            log.info("Gots upperLimit of sampleRecord: {}, result: {}", sampleRecord, result);

            if (!result.isEmpty()) {
                final Map<String, Object> resultRecord = safeMap(result.get(0));
                return safeList(config.getFaker().getColumnNames()).stream()
                        .collect(toMap(columnName -> columnName, columnName -> {
                            Object columnValue = resultRecord.get(columnName);
                            // fix: Phoenix default to Upper
                            if (isNull(columnValue)) {
                                columnValue = resultRecord.get(columnName.toUpperCase());
                            }
                            // 为空则表示 fakeEndDate 之后无数据, 也即无法限制生成的 fakeValue 上限制.
                            if (isNull(columnValue)) {
                                return Double.MAX_VALUE;
                            } else if (columnValue instanceof Number) {
                                return ((BigDecimal) columnValue).setScale(4).doubleValue();
                            }
                            log.warn("Unable parse upperLimit of sampleRecord: {}, result: {}, columnValue: {}", sampleRecord,
                                    result, columnValue);
                            return Double.MAX_VALUE;
                        }));
            }

            return emptyMap();
        }

        /**
         * 获取生成的累计值的下限 (fakeStartDate 之前的最大值). </br>
         * for example:
         * 
         * <code>
         * select rount(max(to_number("activePower")),4) as activePower,rount(max(to_number("reactivePower")),4) as reactivePower
         * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,202210192114' and "ROW"<='11111277,ELE_P,134,01,202210220834' limit 10;
         * </code>
         * 
         * @throws ParseException
         */
        private Map<String, Double> getLowerLimitFakeValues(Map<String, Object> sampleRecord) throws ParseException {
            final String sampleRowKey = (String) sampleRecord.get(config.getRowKey().getName());
            final Map<String, String> sampleRowKeyParts = config.getRowKey().from(sampleRowKey);

            // 从 fakeStartDate 开始向前取任意时间点作为开始时间(这里硬编码为1个周期),
            final Date lowerLimitStartDate = getOffsetDate(getFakeStartDate(), config.getFaker().getSampleLastDatePattern(),
                    config.getFaker().getSampleLastDateAmount() * -1);
            final Date lowerLimitEndDate = getOffsetDate(getFakeStartDate(), config.getFaker().getSampleLastDatePattern(), 0);
            final String lowerLimitStartRowKey = generateRowKey(sampleRowKeyParts, lowerLimitStartDate);
            final String lowerLimitEndRowKey = generateRowKey(sampleRowKeyParts, lowerLimitEndDate);

            StringBuilder columns = new StringBuilder();
            for (String columnName : safeList(config.getFaker().getColumnNames())) {
                columns.append("round(max(to_number(\"");
                columns.append(columnName);
                columns.append("\")),4) as ");
                columns.append(columnName);
                columns.append(",");
            }
            columns.delete(columns.length() - 1, columns.length());

            String queryLimitSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                    config.getTableNamespace(), config.getTableName(), lowerLimitStartRowKey, lowerLimitEndRowKey);
            log.debug("Query lowerLimit sql: {}", queryLimitSql);

            List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryLimitSql));
            log.info("Gots lowerLimit of sampleRecord: {}, result: {}", sampleRecord, result);

            if (!result.isEmpty()) {
                final Map<String, Object> resultRecord = safeMap(result.get(0));
                return safeList(config.getFaker().getColumnNames()).stream()
                        .collect(toMap(columnName -> columnName, columnName -> {
                            Object columnValue = resultRecord.get(columnName);
                            if (isNull(columnValue)) { // fix: Phoenix default
                                                       // to Upper
                                columnValue = resultRecord.get(columnName.toUpperCase());
                            }
                            // 为空则表示 fakeStartDate 之前无数据, 也即无法限制生成的 fakeValue
                            // 下限制.
                            if (isNull(columnValue)) {
                                return Double.MIN_VALUE;
                            } else if (columnValue instanceof Number) {
                                return ((BigDecimal) columnValue).setScale(4).doubleValue();
                            }
                            log.warn("Unable parse lowerLimit of sampleRecord: {}, result: {}, columnValue: {}", sampleRecord,
                                    result, columnValue);
                            return Double.MIN_VALUE;
                        }));
            }

            return emptyMap();
        }

        private Object generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double incrementCount,
                double incrementValue,
                Double upperLimitValue,
                Double lowerLimitValue,
                Object valueObj) {
            double value = -1d;
            if (valueObj instanceof String) {
                value = parseDouble((String) valueObj);
                return generateFakeValue(columnName, sampleRecord, incrementCount, incrementValue, upperLimitValue,
                        lowerLimitValue, value).toPlainString();
            } else if (valueObj instanceof Number) {
                value = ((Number) valueObj).doubleValue();
                return generateFakeValue(columnName, sampleRecord, incrementCount, incrementValue, upperLimitValue,
                        lowerLimitValue, value).intValue();
            }
            return value;
        }

        private BigDecimal generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double incrementCount,
                double incrementValue,
                Double upperLimitValue,
                Double lowerLimitValue,
                double value) {

            // Gets or initial
            AtomicDouble lastMaxFakeValue = obtainLastMaxFakeValue(columnName, sampleRecord, lowerLimitValue);

            // upperLimitValue 不为 Double.MAX 则表示 fakeEndDate 之后有数据, 也即限制生成的
            // fakeValue 上限制.
            //
            // lowerLimitValue 不为 Double.MIN 则表示 fakeStartDate 之前有数据, 也即最好使用
            // (upper-lower)/count 作为平均增量
            if (upperLimitValue < Double.MAX_VALUE && lowerLimitValue > Double.MIN_VALUE) {
                double spanAvgIncrementValue = (upperLimitValue - lowerLimitValue) / incrementCount;
                if (spanAvgIncrementValue > 0) { // safety-check
                    log.info(
                            "Using spanAvgIncrementValue: {}, upperLimitValue: {}, lowerLimitValue: {}, incrementCount: {}, value: {}, sampleRecord: {}",
                            spanAvgIncrementValue, upperLimitValue, lowerLimitValue, incrementCount, sampleRecord);
                    incrementValue = spanAvgIncrementValue;
                }
            }

            // Best effort retry generating.
            // int i = 0;
            // int maxAttempts =
            // config.getCumulativeFaker().getMaxAttempts();
            // do {
            double fakeAmount = nextDouble(config.getFaker().getValueMinRandomPercent() * incrementValue,
                    config.getFaker().getValueMaxRandomPercent() * incrementValue);
            double fakeValue = lastMaxFakeValue.get() + fakeAmount;
            // if (i > 0) {
            // log.info("Re-generating {} of incrementValue: {}, value: {},
            // fakeValue: {}, lastMaxFakeValue: {}, sampleRecord: {}",
            // i, incrementValue, value, fakeValue, lastMaxFakeValue,
            // sampleRecord);
            // Thread.yield();
            // }
            // } while (fakeValue < lastMaxFakeValue.get() && i++ <
            // maxAttempts);

            // Check if max retries are exceeded.
            // if (i >= (maxAttempts - 1)) {
            // fakeValue = value +
            // config.getCumulativeFaker().getFallbackFakeAmountValue();
            // log.warn("The best-effort attempts {} is still
            // unsatisfactory. -
            // incrementValue: {}, value: {}, sampleRecord: {}",
            // maxAttempts, fakeValue, incrementValue, value, sampleRecord);
            // }

            // 检查是否保持递增
            if (fakeValue < lastMaxFakeValue.get()) {
                throw new IllegalFakeValueToolsException(format(
                        "Should not be here, must be fakeValue >= lastMaxFakeValue, but %s >= %s ?, valueMinRandomPercent: %s, valueMaxRandomPercent: %s, sampleRecord: %s",
                        fakeValue, lastMaxFakeValue, config.getFaker().getValueMinRandomPercent(),
                        config.getFaker().getValueMaxRandomPercent(), sampleRecord));
            }
            // 检查是否超过 fakeEndDate 之后的实际数据最小值限制.
            if (fakeValue >= upperLimitValue) {
                throw new IllegalFakeValueToolsException(format(
                        "Invalid generated fakeValue, must be fakeValue < upperLimitValue, but %s < %s ?, valueMinRandomPercent: %s, valueMaxRandomPercent: %s, sampleRecord: %s",
                        fakeValue, upperLimitValue, config.getFaker().getValueMinRandomPercent(),
                        config.getFaker().getValueMaxRandomPercent(), sampleRecord));
            }
            log.info("Update lastMaxFakeValue - incrementValue: {}, fakeValue: {}, value: {}, sampleRecord: {}", incrementValue,
                    fakeValue, value, sampleRecord);

            // 保持 lastMaxFakeValue 有效(影响下次递增).
            lastMaxFakeValue.set(fakeValue);

            return new BigDecimal(fakeValue).setScale(4, BigDecimal.ROUND_HALF_UP);
        }

        private AtomicDouble obtainLastMaxFakeValue(String columnName, Map<String, Object> sampleRecord, double lowerLimitValue) {
            AtomicDouble lastMaxFakeValue = lastMaxFakeValues.get(columnName);
            if (isNull(lastMaxFakeValue)) {
                synchronized (this) {
                    lastMaxFakeValue = lastMaxFakeValues.get(columnName);
                    if (isNull(lastMaxFakeValue)) {
                        lastMaxFakeValues.put(columnName, lastMaxFakeValue = new AtomicDouble(-Double.MAX_VALUE));
                    }
                }
            }
            if (lastMaxFakeValue.get() == -Double.MAX_VALUE) {
                log.info("Initial lastMaxFakeValue - columnName: {}, lowerLimitValue: {}, sampleRecord: {}", columnName,
                        lowerLimitValue, sampleRecord);
                lastMaxFakeValue.set(lowerLimitValue);
            }
            return lastMaxFakeValue;
        }

    }

    /**
     * 获取之前某段历史的增量平均值, 用于尽量确保生成均匀的递增值.
     * 
     * for example:
     * 
     * <code>
     * select round((max(to_number("activePower"))-min(to_number("activePower")))/7,4) as activePower,round((max(to_number("reactivePower"))-min(to_number("reactivePower")))/7,4) reactivePower
     * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,202210132114' and "ROW"<='11111277,ELE_P,134,01,202210202114' limit 10;
     * </code>
     * 
     * @throws ParseException
     */
    protected Map<String, Double> getBeforeAverageIncrementValues(Map<String, Object> sampleRecord) throws ParseException {
        final String sampleRowKey = (String) sampleRecord.get(config.getRowKey().getName());
        final Map<String, String> sampleRowKeyParts = config.getRowKey().from(sampleRowKey);
        final String sampleRowKeyDateString = sampleRowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);
        final Date sampleBeforeAvgEndDate = DateUtils2.parseDate(sampleRowKeyDateString, rowKeyDatePattern);

        final Date sampleBeforeAvgStartDate = getOffsetRowKeyDate(sampleRowKeyDateString, sampleRowKeyParts,
                -config.getFaker().getMonotoneIncrease().getSampleBeforeAverageDateAmount());
        final String sampleBeforeAvgStartRowKey = generateRowKey(sampleRowKeyParts, sampleBeforeAvgStartDate);

        StringBuilder columns = new StringBuilder();
        for (String columnName : safeList(config.getFaker().getColumnNames())) {
            columns.append("round((max(to_number(\"");
            columns.append(columnName);
            columns.append("\"))-min(to_number(\"");
            columns.append(columnName);
            columns.append("\")))/");
            columns.append(Math.abs(config.getFaker().getMonotoneIncrease().getSampleBeforeAverageDateAmount()));
            columns.append(",4) as ");
            columns.append(columnName);
            columns.append(",");
        }
        // 平均值对应总条数, 之后再除以此值, 即可得到平均每条数据的增量.
        columns.append("count(\"");
        columns.append(config.getRowKey().getName());
        columns.append("\") as ");
        columns.append(SQL_COUNT_KEY);

        String queryAvgSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                config.getTableNamespace(), config.getTableName(), sampleBeforeAvgStartRowKey, sampleRowKey);
        log.debug("Query sample before average sql: {}", queryAvgSql);

        List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryAvgSql));
        log.info("Gots average increments of sampleRecord: {}, result: {}", sampleRecord, result);

        if (!result.isEmpty()) {
            final Map<String, Object> resultRecord = safeMap(result.get(0));
            final Long count = (Long) resultRecord.get(SQL_COUNT_KEY);

            Map<String, Double> incrementValues = safeList(config.getFaker().getColumnNames()).stream()
                    .collect(toMap(columnName -> columnName, columnName -> {
                        Object columnValue = resultRecord.get(columnName);
                        // fix: Phoenix default to Upper
                        if (isNull(columnValue)) {
                            columnValue = resultRecord.get(columnName.toUpperCase());
                        }
                        if (columnValue instanceof String) {
                            return parseDouble((String) columnValue) / count;
                        } else if (columnValue instanceof BigDecimal) {
                            return ((BigDecimal) columnValue).doubleValue() / count;
                        } else if (columnValue instanceof Double) {
                            return ((Double) columnValue) / count;
                        } else if (columnValue instanceof Float) {
                            return ((Float) columnValue).doubleValue() / count;
                        } else if (columnValue instanceof Long) {
                            return ((Long) columnValue).doubleValue() / count;
                        } else if (columnValue instanceof Integer) {
                            return ((Integer) columnValue).doubleValue() / count;
                        } else if (columnValue instanceof Short) {
                            return ((Short) columnValue).doubleValue() / count;
                        }
                        throw new IllegalArgumentException(format("Unable to parse columnValue of '%s'", columnValue));
                    }));

            // 例如 count 是过去 getSampleBeforeAverageDateAmount()=3 天的总记录数, 这里要的 1
            // 天平均的记录数.
            double cycles = DateUtils2.getDistanceOf(sampleBeforeAvgStartDate, sampleBeforeAvgEndDate,
                    config.getFaker().getSampleLastDatePattern());
            incrementValues.put(AVG_COUNT_KEY, count / cycles);
            return incrementValues;
        }

        return emptyMap();
    }

    private static final String SQL_COUNT_KEY = "COUNT";
    private static final String AVG_COUNT_KEY = "AVG_COUNT";

}
