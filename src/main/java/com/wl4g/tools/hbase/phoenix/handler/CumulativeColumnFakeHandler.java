package com.wl4g.tools.hbase.phoenix.handler;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomUtils.nextDouble;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addHours;
import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addMonths;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.apache.commons.lang3.time.DateUtils.parseDate;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.util.concurrent.AtomicDouble;
import com.wl4g.infra.common.math.Maths;
import com.wl4g.tools.hbase.phoenix.config.PhoenixFakeProperties;
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
public class CumulativeColumnFakeHandler implements InitializingBean, ApplicationRunner {

    private @Autowired PhoenixFakeProperties config;
    private @Autowired JdbcTemplate jdbcTemplate;
    private final AtomicInteger totalOfAll = new AtomicInteger(0); // e.g:devices-total
    private final AtomicInteger completedOfAll = new AtomicInteger(0);
    private ExecutorService executor;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.executor = Executors.newFixedThreadPool((config.getThreadPools() <= 1) ? 1 : config.getThreadPools());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // see: https://www.baeldung.com/apache-commons-csv
        log.info("Loading metadata from csv ...");

        Reader in = new FileReader(config.getMetaCsvFile());
        CSVParser records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        if (nonNull(records)) {
            for (CSVRecord record : records) {
                log.info("Processing meta record : {}", record);
                String fetchStartRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getSample().getStartDate());
                String fetchEndRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getSample().getEndDate());
                executor.submit(new ProcessTask(fetchStartRowKey, fetchEndRowKey));
            }
            log.info("Waiting for running completion with {}sec ...", config.getAwaitSeconds());
            executor.awaitTermination(config.getAwaitSeconds(), TimeUnit.SECONDS);

            log.info("Processed all completed of {}/{}", completedOfAll.get(), totalOfAll.get());
            executor.shutdown();
        }
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String fetchStartRowKey;
        private String fetchEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);
        private final AtomicDouble lastFakeValue = new AtomicDouble(0);

        @Override
        public void run() {
            // Load history raw records.
            // e.g: 11111111,ELE_P,111,08,20170729165254063
            String queryRawSql = format("select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' limit %s",
                    config.getTableNamespace(), config.getTableName(), fetchStartRowKey, fetchEndRowKey, config.getMaxLimit());

            log.info("Fetching: {}", queryRawSql);
            final List<Map<String, Object>> fromRecords = jdbcTemplate.queryForList(queryRawSql);

            // Transform generate new records.
            safeList(fromRecords).parallelStream().map(record -> {
                Map<String, Object> newRecord = new HashMap<>();
                try {
                    // Gets value bias based on history data samples.
                    Map<String, Double> offsetAmounts = getOffsetAmountsWithHistory(record);
                    log.info("Offset amount: {}", offsetAmounts);

                    // Generate random fake new record.
                    for (Map.Entry<String, Object> e : safeMap(record).entrySet()) {
                        String columName = e.getKey();
                        Object value = e.getValue();
                        Double offsetAmount = offsetAmounts.get(columName);

                        if (eqIgnCase(columName, config.getRowKey().getName())) {
                            String newRowKey = generateFakeRowKey((String) value);
                            newRecord.put(columName, newRowKey);
                        } else {
                            if (nonNull(offsetAmount)) {
                                Double fakeValue = generateFakeValue(record, offsetAmount, (String) value);
                                newRecord.put(columName, fakeValue);
                            } else if (!config.getCumulative().getColumnNames().contains(columName)) {
                                newRecord.put(columName, value);
                            } else {
                                log.warn("TODO 列 '{}' 属于累计值，无法直接使用历史值作为 Fake 值, 必须知道增量才有意义, 或将此列从累计列移除.", columName);
                            }
                        }
                        totalOfAll.incrementAndGet();
                    }
                } catch (Exception e) {
                    if (config.isErrorContinue()) {
                        log.warn(format("Could not generate for %s. - %s", record, e.getMessage()));
                    } else {
                        throw new IllegalArgumentException(e);
                    }
                }
                return newRecord;
            }).forEach(newRecord -> {
                // Save to HBase to table.
                try {
                    StringBuilder upsertSql = new StringBuilder(
                            format("upsert into \"%s\".\"%s\" (", config.getTableNamespace(), config.getTableName()));
                    safeMap(newRecord).forEach((columName, value) -> {
                        upsertSql.append("\"");
                        upsertSql.append(columName);
                        upsertSql.append("\",");
                    });
                    upsertSql.delete(upsertSql.length() - 1, upsertSql.length());
                    upsertSql.append(") values (");
                    safeMap(newRecord).forEach((columName, value) -> {
                        upsertSql.append("'");
                        upsertSql.append(value);
                        upsertSql.append("',");
                    });
                    upsertSql.delete(upsertSql.length() - 1, upsertSql.length());
                    upsertSql.append(")");

                    log.info("Executing: {}", upsertSql);
                    if (!config.isDryRun()) {
                        jdbcTemplate.execute(upsertSql.toString());
                    }

                    completed.incrementAndGet();
                    completedOfAll.incrementAndGet();
                } catch (Exception e) {
                    if (config.isErrorContinue()) {
                        log.warn(format("Unable to write to htable of : %s", newRecord), e);
                    } else {
                        throw new IllegalArgumentException(e);
                    }
                }
            });

            log.info("Processed completed of {}/{}/{}/{}", completed.get(), fromRecords.size(), completedOfAll.get(),
                    totalOfAll.get());
        }

        private Double generateFakeValue(Map<String, Object> record, double offsetAmount, String valueString) {
            double fakeValue = 0d, value = parseDouble(valueString);
            // 最大努力重试
            int i = 0, factor = 0, maxAttempts = config.getGenerator().getMaxAttempts();
            do {
                double fakeAmount = nextDouble(config.getGenerator().getValueRandomMinPercent() * offsetAmount,
                        // Since value increments must be satisfied, each retry
                        // is multiple by a factor in order to accelerate
                        // generation to a number greater than the previous
                        // value.
                        config.getGenerator().getValueRandomMaxPercent() * offsetAmount * ++factor);
                fakeValue = Maths.round(value + fakeAmount, 4).doubleValue();
                if (i > 0) {
                    log.warn("Retry generating incremented fakeValue of offsetAmount: {}, value: {}, record: {}", offsetAmount,
                            value, record);
                    Thread.yield();
                }
            } while (i++ < maxAttempts && fakeValue < lastFakeValue.get());

            // 检查最大重试是否失败
            if (i >= (maxAttempts - 1)) {
                log.warn(
                        "The best-effort attempts {} is still unsatisfactory, using the default value: {}. - offsetAmount: {}, value: {}, record: {}",
                        maxAttempts, 0, offsetAmount, value, record);
                fakeValue = value + config.getGenerator().getFallbackFakeAmountValue();
            }

            // 尽量保持 lastFakeValue 有效(影响下次递增).
            if ((fakeValue > 0 && fakeValue >= value) || fakeValue > lastFakeValue.get()) {
                lastFakeValue.set(fakeValue);
            }

            return fakeValue;
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
    Map<String, Double> getOffsetAmountsWithHistory(Map<String, Object> record) throws ParseException {
        final String rowKeyDatePattern = config.getRowKey().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();
        final String rowKey = (String) record.get(config.getRowKey().getName());
        final Map<String, String> rowKeyParts = config.getRowKey().from(rowKey);
        // Gets sampling data record date.
        final Date sampleStartDate = getOffsetRowKeyDate(rowKeyDatePattern, rowKeyParts, config.getSample().getLastDateAmount());
        final String sampleStartRowKey = generateRowKey(rowKeyDatePattern, rowKeyParts, rowKey, sampleStartDate);

        StringBuilder columns = new StringBuilder();
        for (String columnName : safeList(config.getCumulative().getColumnNames())) {
            columns.append("round((max(to_number(\"");
            columns.append(columnName);
            columns.append("\"))-min(to_number(\"");
            columns.append(columnName);
            columns.append("\")))/");
            columns.append(Math.abs(config.getSample().getLastDateAmount()));
            columns.append(",4) as ");
            columns.append(columnName);
            columns.append(",");
        }
        columns.delete(columns.length() - 1, columns.length());

        String queryOffsetSql = format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", columns,
                config.getTableNamespace(), config.getTableName(), sampleStartRowKey, rowKey);
        log.debug("Fetching offset: {}", queryOffsetSql);

        List<Map<String, Object>> result = safeList(jdbcTemplate.queryForList(queryOffsetSql));
        log.debug("Gots offset amount of record: {}, result: {}", record, result);

        if (!result.isEmpty()) {
            final Map<String, Object> resultRecord = safeMap(result.get(0));
            return safeList(config.getCumulative().getColumnNames()).stream()
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

    String generateFakeRowKey(final String rowKey) throws ParseException {
        final String rowKeyDatePattern = config.getRowKey().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();
        final Map<String, String> rowKeyParts = config.getRowKey().from(rowKey);
        // Gets tampered with target record date.
        final Date fakeDate = getOffsetRowKeyDate(rowKeyDatePattern, rowKeyParts, config.getGenerator().getRowKeyDateAmount());
        return generateRowKey(rowKeyDatePattern, rowKeyParts, rowKey, fakeDate);
    }

    String generateRowKey(
            final String rowKeyDatePattern,
            final Map<String, String> rowKeyParts,
            final String rowKey,
            final Date rowDate) throws ParseException {
        // Gets tampered with target record date.
        return config.getRowKey().to(rowKeyParts, formatDate(rowDate, rowKeyDatePattern));
    }

    /**
     * Gets offset date from rowKey.
     * 
     * @throws ParseException
     */
    Date getOffsetRowKeyDate(final String rowKeyDatePattern, final Map<String, String> rowKeyParts, int dateAmount)
            throws ParseException {
        String rowKeyDateString = rowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);
        Date date = parseDate(rowKeyDateString, rowKeyDatePattern);
        switch (config.getGenerator().getRowKeyDatePattern()) {
        case "yy":
        case "y":
        case "YY":
        case "Y":
            date = addYears(date, dateAmount);
            break;
        case "MM":
        case "M":
            date = addMonths(date, dateAmount);
            break;
        case "dd":
        case "d":
        case "DD":
        case "D":
            date = addDays(date, dateAmount);
            break;
        case "HH":
        case "H":
        case "hh":
        case "h":
            date = addHours(date, dateAmount);
            break;
        case "mm":
        case "m":
            date = addMinutes(date, dateAmount);
            break;
        case "ss":
        case "s":
            date = addSeconds(date, dateAmount);
            break;
        }
        return date;
    }

}
