package com.wl4g.tools.hbase.phoenix.modules.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static com.wl4g.infra.common.math.Maths.round;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextDouble;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.wl4g.tools.hbase.phoenix.config.ToolsProperties.RunnerProvider;
import com.wl4g.tools.hbase.phoenix.exception.IllegalFakeValueToolsException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple Fake 数据处理器. 如有功功率值, 此列的数值没有递增特性.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public class SimplePhoenixTableFaker extends PhoenixTableFaker {

    @Override
    protected RunnerProvider provider() {
        return RunnerProvider.SIMPLE_FAKER;
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

        @Override
        public void run() {
            try {
                List<Map<String, Object>> sampleRecords = fetchSampleRecords(sampleStartRowKey, sampleEndRowKey);

                // 非递增要求, 可使用 parallelStream
                safeList(sampleRecords).parallelStream().map(sampleRecord -> {
                    Map<String, Object> newRecord = new HashMap<>();
                    try {
                        // Generate random fake new record.
                        for (Map.Entry<String, Object> e : safeMap(sampleRecord).entrySet()) {
                            String columnName = e.getKey();
                            Object value = e.getValue();

                            if (eqIgnCase(columnName, config.getRowKey().getName())) {
                                newRecord.put(columnName, generateFakeRowKey((String) value));
                            } else {
                                if (!config.getFaker().getColumnNames().contains(columnName)) {
                                    newRecord.put(columnName, value);
                                } else {
                                    Object fakeValue = generateFakeValue(columnName, sampleRecord, value);
                                    newRecord.put(columnName, fakeValue);
                                }
                            }
                        }
                        totalOfAll.incrementAndGet();
                    } catch (Exception e) {
                        if (config.isErrorContinue() && !(e instanceof IllegalFakeValueToolsException)) {
                            log.warn(format("Could not generate for %s.", sampleRecord), e);
                        } else {
                            throw new IllegalStateException(e);
                        }
                    }
                    return newRecord;
                }).forEach(newRecord -> {
                    executeUpdateToHTable(newRecord);
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

        private Object generateFakeValue(String columnName, Map<String, Object> sampleRecord, Object valueObj) {
            double value = -1d, min = config.getFaker().getValueMinRandomPercent(),
                    max = config.getFaker().getValueMaxRandomPercent();
            if (valueObj instanceof String) {
                value = parseDouble((String) valueObj);
                return round(nextDouble(min * value, max * value), 4).toString();
            } else if (valueObj instanceof Integer) {
                value = (Integer) valueObj;
                return round(nextDouble(min * value, max * value), 4).intValue();
            } else if (valueObj instanceof Long) {
                value = (Long) valueObj;
                return round(nextDouble(min * value, max * value), 4).longValue();
            } else if (valueObj instanceof Float) {
                value = (Float) valueObj;
                return round(nextDouble(min * value, max * value), 4).floatValue();
            } else if (valueObj instanceof Double) {
                value = (Double) valueObj;
                return round(nextDouble(min * value, max * value), 4).doubleValue();
            } else if (valueObj instanceof BigDecimal) {
                value = ((BigDecimal) valueObj).doubleValue();
                return round(nextDouble(min * value, max * value), 4);
            }
            return value;
        }

    }

}
