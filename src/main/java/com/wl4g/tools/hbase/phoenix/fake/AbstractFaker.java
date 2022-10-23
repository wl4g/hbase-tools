package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addHours;
import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addMonths;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.apache.commons.lang3.time.DateUtils.parseDate;

import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wl4g.tools.hbase.phoenix.config.PhoenixFakeProperties;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract fake records handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class AbstractFaker implements InitializingBean, DisposableBean, ApplicationRunner {

    protected @Autowired PhoenixFakeProperties config;
    protected @Autowired JdbcTemplate jdbcTemplate;
    protected final AtomicInteger totalOfAll = new AtomicInteger(0); // e.g:devices-total
    protected final AtomicInteger completedOfAll = new AtomicInteger(0);
    protected ExecutorService executor;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isActive()) {
            return;
        }
        this.executor = Executors.newFixedThreadPool((config.getThreadPools() <= 1) ? 1 : config.getThreadPools());
    }

    @Override
    public void destroy() throws Exception {
        if (!isActive()) {
            return;
        }
        log.info("Processed all completed of {}/{}", completedOfAll.get(), totalOfAll.get());
        executor.shutdown();
    }

    protected abstract FakeProvider provider();

    private boolean isActive() {
        return provider() == config.getProvider();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isActive()) {
            return;
        }

        // see: https://www.baeldung.com/apache-commons-csv
        log.info("Loading metadata from csv ...");

        Reader in = new FileReader(config.getMetaCsvFile());
        CSVParser metaRecords = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        if (nonNull(metaRecords)) {
            for (CSVRecord record : metaRecords) {
                log.info("Processing meta record : {}", record);

                String sampleStartRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getSampleStartDate());
                String sampleEndRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getSampleEndDate());
                executor.submit(newProcessTask(sampleStartRowKey, sampleEndRowKey));
            }

            log.info("Waiting for running completion with {}sec ...", config.getAwaitSeconds());
            executor.awaitTermination(config.getAwaitSeconds(), TimeUnit.SECONDS);
        }
    }

    protected abstract Runnable newProcessTask(String sampleStartRowKey, String sampleEndRowKey);

    protected List<Map<String, Object>> fetchSampleRecords(String sampleStartRowKey, String sampleEndRowKey) {
        // e.g: 11111111,ELE_P,111,08,20170729165254063
        String queryRawSql = format(
                "select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' order by \"ROW\" asc limit %s",
                config.getTableNamespace(), config.getTableName(), sampleStartRowKey, sampleEndRowKey, config.getMaxLimit());
        log.info("Fetching: {}", queryRawSql);
        return safeList(jdbcTemplate.queryForList(queryRawSql));
    }

    protected String generateFakeRowKey(final String rowKey) throws ParseException {
        final String rowKeyDatePattern = config.getRowKey().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();
        final Map<String, String> rowKeyParts = config.getRowKey().from(rowKey);
        final String rowKeyDateString = rowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);

        final Date fakeDate = getOffsetRowKeyDate(rowKeyDatePattern, rowKeyDateString, rowKeyParts,
                config.getGenerateRowKeyDateAmount());

        return generateRowKey(rowKeyDatePattern, rowKeyParts, rowKey, fakeDate);
    }

    protected String generateRowKey(
            final String rowKeyDatePattern,
            final Map<String, String> rowKeyParts,
            final String rowKey,
            final Date rowDate) throws ParseException {
        // Gets tampered with sample record date.
        return config.getRowKey().to(rowKeyParts, formatDate(rowDate, rowKeyDatePattern));
    }

    /**
     * Gets offset date by rowKey.
     * 
     * @throws ParseException
     */
    protected Date getOffsetRowKeyDate(
            final String rowKeyDatePattern,
            final String rowKeyDateString,
            final Map<String, String> rowKeyParts,
            int dateAmount) throws ParseException {

        Date date = parseDate(rowKeyDateString, rowKeyDatePattern);
        switch (config.getGenerateRowKeyDatePattern()) {
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

    protected void writeToHTable(Map<String, Object> newRecord) {
        // Save to HBase to table.
        try {
            StringBuilder upsertSql = new StringBuilder(
                    format("upsert into \"%s\".\"%s\" (", config.getTableNamespace(), config.getTableName()));
            safeMap(newRecord).forEach((columnName, value) -> {
                upsertSql.append("\"");
                upsertSql.append(columnName);
                upsertSql.append("\",");
            });
            upsertSql.delete(upsertSql.length() - 1, upsertSql.length());
            upsertSql.append(") values (");
            safeMap(newRecord).forEach((columnName, value) -> {
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

            completedOfAll.incrementAndGet();
        } catch (Exception e) {
            if (config.isErrorContinue()) {
                log.warn(format("Unable write to htable of : %s", newRecord), e);
            } else {
                throw new IllegalStateException(e);
            }
        }
    }

    public static enum FakeProvider {
        SIMPLE, CUMULATIVE;
    }

}
