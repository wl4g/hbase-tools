package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.isTrue;
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
import com.wl4g.infra.common.math.Maths;
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
public class CumulativeColumnFaker extends AbstractFaker {

    @Override
    protected FakeProvider provider() {
        return FakeProvider.CUMULATIVE;
    }

    @Override
    protected Runnable newProcessTask(String fetchStartRowKey, String fetchEndRowKey) {
        return new ProcessTask(fetchStartRowKey, fetchEndRowKey);
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String fetchStartRowKey;
        private String fetchEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);
        private final Map<String, AtomicDouble> lastMaxFakeValues = new HashMap<>();

        @Override
        public void run() {
            try {
                // Load history records.
                // e.g: 11111111,ELE_P,111,08,20170729165254063
                String queryRawSql = format(
                        "select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' order by \"ROW\" asc limit %s",
                        config.getTableNamespace(), config.getTableName(), fetchStartRowKey, fetchEndRowKey,
                        config.getMaxLimit());
                log.info("Fetching: {}", queryRawSql);
                List<Map<String, Object>> sampleRecords = jdbcTemplate.queryForList(queryRawSql);

                // rowKey 中时间是递增, 无法使用 parallelStream
                safeList(sampleRecords).stream().map(sampleRecord -> {
                    Map<String, Object> newRecord = new HashMap<>();
                    try {
                        // Gets value bias based on history data samples.
                        Map<String, Double> offsetAmounts = getOffsetAmountsWithHistory(sampleRecord);
                        log.info("Offset amount: {}", offsetAmounts);

                        // Generate random fake new record.
                        for (Map.Entry<String, Object> e : safeMap(sampleRecord).entrySet()) {
                            String columnName = e.getKey();
                            Object value = e.getValue();
                            Double offsetAmount = offsetAmounts.get(columnName);

                            if (eqIgnCase(columnName, config.getRowKey().getName())) {
                                newRecord.put(columnName, generateFakeRowKey((String) value));
                            } else {
                                if (nonNull(offsetAmount)) {
                                    Object fakeValue = generateFakeValue(columnName, sampleRecord, offsetAmount, value);
                                    newRecord.put(columnName, fakeValue);
                                } else if (!config.getCumulativeFaker().getColumnNames().contains(columnName)) {
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
                log.error(
                        format("Failed to process of fetchStartRowKey: %s, fetchEndRowKey: %s", fetchStartRowKey, fetchEndRowKey),
                        e2);
                if (!config.isErrorContinue()) {
                    throw new IllegalStateException(e2);
                }
            }
        }

        private Object generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double offsetAmount,
                Object valueObj) {

            double value = -1d;
            if (valueObj instanceof String) {
                value = parseDouble((String) valueObj);
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value).toString();
            } else if (valueObj instanceof Integer) {
                value = (Integer) valueObj;
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value).intValue();
            } else if (valueObj instanceof Long) {
                value = (Long) valueObj;
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value).longValue();
            } else if (valueObj instanceof Float) {
                value = (Float) valueObj;
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value).floatValue();
            } else if (valueObj instanceof Double) {
                value = (Double) valueObj;
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value).doubleValue();
            } else if (valueObj instanceof BigDecimal) {
                value = ((BigDecimal) valueObj).doubleValue();
                return generateFakeValue(columnName, sampleRecord, offsetAmount, value);
            }
            return value;
        }

        private BigDecimal generateFakeValue(
                String columnName,
                Map<String, Object> sampleRecord,
                double offsetAmount,
                double value) {

            // Gets or initial
            AtomicDouble lastMaxFakeValue = getOrInitLastMaxFakeValue(columnName, sampleRecord, value);

            // Best effort retry generating.
            // int i = 0;
            // int maxAttempts = config.getGenerator().getMaxAttempts();
            // do {
            double fakeAmount = nextDouble(config.getGenerator().getValueRandomMinPercent() * offsetAmount,
                    // Since value increments must be satisfied, each retry
                    // is multiple by a factor in order to accelerate
                    // generation to a number greater than the previous
                    // value.
                    config.getGenerator().getValueRandomMaxPercent() * offsetAmount);
            double fakeValue = Maths.round(lastMaxFakeValue.get() + fakeAmount, 4).doubleValue();
            // if (i > 0) {
            // log.info("Re-generating {} of offsetAmount: {}, value: {},
            // fakeValue: {}, lastMaxFakeValue: {}, sampleRecord: {}",
            // i, offsetAmount, value, fakeValue, lastMaxFakeValue,
            // sampleRecord);
            // Thread.yield();
            // }
            // } while (fakeValue < lastMaxFakeValue.get() && i++ <
            // maxAttempts);

            // Check if max retries are exceeded.
            // if (i >= (maxAttempts - 1)) {
            // fakeValue = value +
            // config.getGenerator().getFallbackFakeAmountValue();
            // log.warn("The best-effort attempts {} is still unsatisfactory. -
            // offsetAmount: {}, value: {}, sampleRecord: {}",
            // maxAttempts, fakeValue, offsetAmount, value, sampleRecord);
            // }

            isTrue(fakeValue > lastMaxFakeValue.get(),
                    "Should not be here, must be fakeValue >= lastMaxFakeValue, but %s >= %s ?", fakeValue, lastMaxFakeValue);

            log.info("Update lastMaxFakeValue - offsetAmount: {}, fakeValue: {}, value: {}, sampleRecord: {}", offsetAmount,
                    fakeValue, value, sampleRecord);

            // 保持 lastMaxFakeValue 有效(影响下次递增).
            lastMaxFakeValue.set(fakeValue);

            return new BigDecimal(fakeValue);
        }

        private AtomicDouble getOrInitLastMaxFakeValue(String columnName, Map<String, Object> sampleRecord, double value) {
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
     * for example:
     * 
     * <pre>
     * select (max(to_number("activePower"))-min(to_number("activePower")))/7 activePower,(max(to_number("reactivePower"))-min(to_number("reactivePower")))/7 reactivePower
     * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,202210132114' and "ROW"<='11111277,ELE_P,134,01,202210202114' limit 10;
     * </pre>
     * 
     * @throws ParseException
     */
    protected Map<String, Double> getOffsetAmountsWithHistory(Map<String, Object> fromRecord) throws ParseException {
        final String rowKeyDatePattern = config.getRowKey().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();
        final String rowKey = (String) fromRecord.get(config.getRowKey().getName());
        final Map<String, String> rowKeyParts = config.getRowKey().from(rowKey);
        // Gets sampling data fromRecord date.
        final Date sampleStartDate = getOffsetRowKeyDate(rowKeyDatePattern, rowKeyParts,
                config.getCumulativeFaker().getOffsetLastDateAmount());
        final String sampleStartRowKey = generateRowKey(rowKeyDatePattern, rowKeyParts, rowKey, sampleStartDate);

        StringBuilder columns = new StringBuilder();
        for (String columnName : safeList(config.getCumulativeFaker().getColumnNames())) {
            columns.append("round((max(to_number(\"");
            columns.append(columnName);
            columns.append("\"))-min(to_number(\"");
            columns.append(columnName);
            columns.append("\")))/");
            columns.append(Math.abs(config.getCumulativeFaker().getOffsetLastDateAmount()));
            columns.append(",4) as ");
            columns.append(columnName);
            columns.append(",");
        }
        columns.delete(columns.length() - 1, columns.length());

        String queryOffsetSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                config.getTableNamespace(), config.getTableName(), sampleStartRowKey, rowKey);
        log.debug("Fetching offset: {}", queryOffsetSql);

        List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryOffsetSql));
        log.debug("Gots offset amount of fromRecord: {}, result: {}", fromRecord, result);

        if (!result.isEmpty()) {
            final Map<String, Object> resultRecord = safeMap(result.get(0));
            return safeList(config.getCumulativeFaker().getColumnNames()).stream()
                    .collect(toMap(columnName -> columnName, columnName -> {
                        Object columnValue = resultRecord.get(columnName);
                        // fix: Phoenix result to Upper
                        columnValue = resultRecord.get(columnName.toUpperCase());
                        if (columnValue instanceof BigDecimal) {
                            return ((BigDecimal) columnValue).doubleValue();
                        } else if (columnValue instanceof String) {
                            return parseDouble((String) columnValue);
                        }
                        throw new IllegalArgumentException(format("Unable to parse columnValue of '%s'", columnValue));
                    }));
        }

        return emptyMap();
    }

}
