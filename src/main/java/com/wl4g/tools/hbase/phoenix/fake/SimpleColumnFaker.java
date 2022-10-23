package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static org.apache.commons.lang3.RandomUtils.nextDouble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.wl4g.infra.common.math.Maths;

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
public class SimpleColumnFaker extends AbstractFaker {

    @Override
    protected FakeProvider provider() {
        return FakeProvider.SIMPLE;
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
                        // Generate random fake new record.
                        for (Map.Entry<String, Object> e : safeMap(sampleRecord).entrySet()) {
                            String columnName = e.getKey();
                            Object value = e.getValue();

                            if (eqIgnCase(columnName, config.getRowKey().getName())) {
                                newRecord.put(columnName, generateFakeRowKey((String) value));
                            } else {
                                if (!config.getCumulativeFaker().getColumnNames().contains(columnName)) {
                                    newRecord.put(columnName, value);
                                } else {
                                    Double fakeValue = generateFakeValue(columnName, sampleRecord, (String) value);
                                    newRecord.put(columnName, fakeValue);
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

        private Double generateFakeValue(String columnName, Map<String, Object> sampleRecord, String valueString) {
            double value = parseDouble(valueString);
            double fakeValue = nextDouble(config.getGenerator().getValueRandomMinPercent() * value,
                    // Since value increments must be satisfied, each retry
                    // is multiple by a factor in order to accelerate
                    // generation to a number greater than the previous
                    // value.
                    config.getGenerator().getValueRandomMaxPercent() * value);
            return Maths.round(fakeValue, 4).doubleValue();
        }

    }

}
