package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
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
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.AllArgsConstructor;
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
public class CumulativeColumnsFaker extends AbstractFaker {

    @Override
    protected FakeProvider provider() {
        return FakeProvider.CUMULATIVE;
    }

    @Override
    protected Runnable newProcessTask(String sampleStartRowKey, String sampleEndRowKey) {
        return new ProcessTask(sampleStartRowKey, sampleEndRowKey);
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String sampleStartRowKey;
        private String sampleEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);
        private final Map<String, AtomicDouble> lastMaxFakeValues = new HashMap<>();

        @Override
        public void run() {
            try {
                List<Map<String, Object>> sampleRecords = fetchSampleRecords(sampleStartRowKey, sampleEndRowKey);

                // rowKey 中时间是递增, 无法使用 parallelStream
                safeList(sampleRecords).stream().map(sampleRecord -> {
                    Map<String, Object> newRecord = new HashMap<>();
                    try {
                        // Gets averages based on before history samples data.
                        Map<String, Double> incrementValues = getBeforeAverageIncrementValues(sampleRecord);
                        log.info("Avg increment values: {}", incrementValues);

                        // Gets generate max-value limit based on after actual
                        // data.
                        Map<String, Double> limitValues = getGenerateFakeLimitMaxValues(sampleRecord);
                        log.info("Limit values: {}", limitValues);

                        // Generate random fake new record.
                        for (Map.Entry<String, Object> e : safeMap(sampleRecord).entrySet()) {
                            String columnName = e.getKey();
                            Object value = e.getValue();
                            Double incrementValue = incrementValues.get(columnName);
                            Double limitMaxValue = limitValues.get(columnName);

                            if (eqIgnCase(columnName, config.getRowKey().getName())) {
                                newRecord.put(columnName, generateFakeRowKey((String) value));
                            } else {
                                if (nonNull(incrementValue)) {
                                    Object fakeValue = generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue,
                                            value);
                                    newRecord.put(columnName, fakeValue);
                                } else if (!config.getColumnNames().contains(columnName)) {
                                    newRecord.put(columnName, value);
                                } else {
                                    log.warn("TODO 列 '{}' 属于累计值，无法直接使用历史值作为 Fake 值, 必须知道增量才有意义, 或将此列从累计列移除.", columnName);
                                }
                            }
                        }
                        totalOfAll.incrementAndGet();
                    } catch (Exception e) {
                        if (config.isErrorContinue()) {
                            log.warn(format("Could not generate for %s.", sampleRecord), e);
                        } else {
                            throw new IllegalStateException(e);
                        }
                    }
                    return newRecord;
                }).forEach(newRecord -> {
                    writeToHTable(newRecord);
                    completed.incrementAndGet();
                });

                log.info("Processed completed of {}/{}/{}/{}", completed.get(), sampleRecords.size(), completedOfAll.get(),
                        totalOfAll.get());
            } catch (Exception e2) {
                log.error(format("Failed to process of sampleStartRowKey: %s, sampleEndRowKey: %s", sampleStartRowKey,
                        sampleEndRowKey), e2);
                if (!config.isErrorContinue()) {
                    throw new IllegalStateException(e2);
                }
            }
        }

        private Object generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double incrementValue,
                Double limitMaxValue,
                Object valueObj) {
            double value = -1d;
            if (valueObj instanceof String) {
                value = parseDouble((String) valueObj);
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value).toPlainString();
            } else if (valueObj instanceof Integer) {
                value = (Integer) valueObj;
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value).intValue();
            } else if (valueObj instanceof Long) {
                value = (Long) valueObj;
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value).longValue();
            } else if (valueObj instanceof Float) {
                value = (Float) valueObj;
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value).floatValue();
            } else if (valueObj instanceof Double) {
                value = (Double) valueObj;
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value).doubleValue();
            } else if (valueObj instanceof BigDecimal) {
                value = ((BigDecimal) valueObj).doubleValue();
                return generateFakeValue(columnName, sampleRecord, incrementValue, limitMaxValue, value);
            }
            return value;
        }

        private BigDecimal generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double incrementValue,
                Double limitMaxValue,
                double value) {

            // Gets or initial
            AtomicDouble lastMaxFakeValue = obtainLastMaxFakeValue(columnName, sampleRecord, value);

            // Best effort retry generating.
            // int i = 0;
            // int maxAttempts = config.getCumulativeFaker().getMaxAttempts();
            // do {
            double fakeAmount = nextDouble(config.getValueMinRandomPercent() * incrementValue,
                    // Since value incrementValues must be satisfied, each retry
                    // is multiple by a factor in order to accelerate
                    // generation to a number greater than the previous
                    // value.
                    config.getValueMaxRandomPercent() * incrementValue);
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
            // log.warn("The best-effort attempts {} is still unsatisfactory. -
            // incrementValue: {}, value: {}, sampleRecord: {}",
            // maxAttempts, fakeValue, incrementValue, value, sampleRecord);
            // }

            // 检查是否保持递增
            if (fakeValue < lastMaxFakeValue.get()) {
                throw new IllegalStateException(
                        format("Should not be here, must be fakeValue >= lastMaxFakeValue, but %s >= %s ?", fakeValue,
                                lastMaxFakeValue));
            }
            // 检查是否超过 fakeEndDate 之后的实际数据最小值限制.
            if (fakeValue >= limitMaxValue) {
                throw new IllegalStateException(
                        format("Invalid generated fakeValue, must be fakeValue < limitMaxValue, but %s < %s ?", fakeValue,
                                limitMaxValue));
            }

            log.info("Update lastMaxFakeValue - incrementValue: {}, fakeValue: {}, value: {}, sampleRecord: {}", incrementValue,
                    fakeValue, value, sampleRecord);

            // 保持 lastMaxFakeValue 有效(影响下次递增).
            lastMaxFakeValue.set(fakeValue);

            return new BigDecimal(fakeValue).setScale(4, BigDecimal.ROUND_HALF_UP);
        }

        private AtomicDouble obtainLastMaxFakeValue(String columnName, Map<String, Object> sampleRecord, double value) {
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
                log.info("Initial lastMaxFakeValue - columnName: {}, value: {}, sampleRecord: {}", columnName, value,
                        sampleRecord);
                lastMaxFakeValue.set(value);
            }
            return lastMaxFakeValue;
        }

    }

    /**
     * 获取之前某时段的历史平均值, 用于尽量保证生成比较均匀的递增值.
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
        final Map<String, String> rowKeyParts = config.getRowKey().from(sampleRowKey);
        final String rowKeyDateString = rowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);

        final Date sampleBeforeAvgStartDate = getOffsetRowKeyDate(rowKeyDateString, rowKeyParts,
                -config.getCumulative().getSampleBeforeAverageDateAmount());
        final String sampleBeforeAvgStartRowKey = generateRowKey(rowKeyParts, sampleRowKey, sampleBeforeAvgStartDate);

        StringBuilder columns = new StringBuilder();
        for (String columnName : safeList(config.getColumnNames())) {
            columns.append("round((max(to_number(\"");
            columns.append(columnName);
            columns.append("\"))-min(to_number(\"");
            columns.append(columnName);
            columns.append("\")))/");
            columns.append(Math.abs(config.getCumulative().getSampleBeforeAverageDateAmount()));
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
            return safeList(config.getColumnNames()).stream().collect(toMap(columnName -> columnName, columnName -> {
                Object columnValue = resultRecord.get(columnName);
                if (isNull(columnValue)) { // fix: Phoenix default to Upper
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
        }

        return emptyMap();
    }

    /**
     * 获取用于限制每次生成累计值都不会超过 fakeEndDate 之后的最小值. </br>
     * for example:
     * 
     * <code>
     * select rount(min(to_number("activePower")),4) as activePower,rount(min(to_number("reactivePower")),4) as reactivePower
     * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,202210220834' and "ROW"<='11111277,ELE_P,134,01,202210230834' limit 10;
     * </code>
     * 
     * @throws ParseException
     */
    protected Map<String, Double> getGenerateFakeLimitMaxValues(Map<String, Object> sampleRecord) throws ParseException {
        final String sampleRowKey = (String) sampleRecord.get(config.getRowKey().getName());
        final Map<String, String> rowKeyParts = config.getRowKey().from(sampleRowKey);

        // 从 fakeEndDate 开始向后取任意时间点作为结束时间(这里硬编码为2个周期),
        final Date limitStartDate = getOffsetDate(fakeEndDate, config.getSampleLastDatePattern(),
                config.getSampleLastDateAmount());
        final Date limitEndDate = getOffsetDate(fakeEndDate, config.getSampleLastDatePattern(),
                config.getSampleLastDateAmount() * 2);
        final String limitStartRowKey = generateRowKey(rowKeyParts, sampleRowKey, limitStartDate);
        final String limitEndRowKey = generateRowKey(rowKeyParts, sampleRowKey, limitEndDate);

        StringBuilder columns = new StringBuilder();
        for (String columnName : safeList(config.getColumnNames())) {
            columns.append("round(min(to_number(\"");
            columns.append(columnName);
            columns.append("\")),4) as ");
            columns.append(columnName);
            columns.append(",");
        }
        columns.delete(columns.length() - 1, columns.length());

        String queryLimitSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                config.getTableNamespace(), config.getTableName(), limitStartRowKey, limitEndRowKey);
        log.debug("Query limit sql: {}", queryLimitSql);

        List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryLimitSql));
        log.info("Gots limit of sampleRecord: {}, result: {}", sampleRecord, result);

        if (!result.isEmpty()) {
            final Map<String, Object> resultRecord = safeMap(result.get(0));
            return safeList(config.getColumnNames()).stream().collect(toMap(columnName -> columnName, columnName -> {
                Object columnValue = resultRecord.get(columnName);
                if (isNull(columnValue)) { // fix: Phoenix default to Upper
                    columnValue = resultRecord.get(columnName.toUpperCase());
                }
                // 为空则表示 fakeEndDate 之后无数据, 也即无法限制生成的 fakeValue 限制.
                return isNull(columnValue) ? Double.MAX_VALUE : ((Double) columnValue);
            }));
        }

        return emptyMap();
    }

    private static final String SQL_COUNT_KEY = "COUNT";

}