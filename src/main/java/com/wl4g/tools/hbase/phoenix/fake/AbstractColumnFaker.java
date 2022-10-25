package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addHours;
import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addMonths;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.apache.commons.lang3.time.DateUtils.parseDate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
public abstract class AbstractColumnFaker implements InitializingBean, DisposableBean, ApplicationRunner {

    protected @Autowired PhoenixFakeProperties config;
    protected @Autowired JdbcTemplate jdbcTemplate;
    protected final AtomicInteger totalOfAll = new AtomicInteger(0); // e.g:devices-total
    protected final AtomicInteger completedOfAll = new AtomicInteger(0);
    protected ExecutorService executor;
    protected String rowKeyDatePattern;
    protected Date fakeStartDate;
    protected Date fakeEndDate;
    protected Map<String, SqlLogFileWriter> sqlLogFileWriters = new ConcurrentHashMap<>(1024);

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isActive()) {
            return;
        }
        this.executor = Executors.newFixedThreadPool((config.getThreadPools() <= 1) ? 1 : config.getThreadPools());
        this.rowKeyDatePattern = config.getRowKey().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();

        // 若为省略日期写法, 则需自动补0
        String complement = "", startDate = config.getStartDate(), endDate = config.getEndDate();
        for (int i = 0; i < rowKeyDatePattern.length() - config.getStartDate().length(); i++) {
            complement += "0";
        }
        complement = "";
        for (int i = 0; i < rowKeyDatePattern.length() - config.getEndDate().length(); i++) {
            complement += "0";
        }
        this.fakeStartDate = parseDate(startDate += complement, rowKeyDatePattern);
        this.fakeEndDate = parseDate(endDate += complement, rowKeyDatePattern);
    }

    @Override
    public void destroy() throws Exception {
        if (!isActive()) {
            return;
        }
        log.info("Processed all completed of {}/{}", completedOfAll.get(), totalOfAll.get());

        safeMap(sqlLogFileWriters).forEach((key, sqlLogWriter) -> {
            try {
                if (nonNull(sqlLogWriter.getUndoSqlWriter())) {
                    sqlLogWriter.getUndoSqlWriter().close();
                }
            } catch (Exception e) {
                log.error(format("Unable to closing undo writer for '%s'", key), e);
            }
            try {
                if (nonNull(sqlLogWriter.getRedoSqlWriter())) {
                    sqlLogWriter.getRedoSqlWriter().close();
                }
            } catch (Exception e) {
                log.error(format("Unable to closing redo writer for '%s'", key), e);
            }
        });
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

        Reader in = new FileReader(new File(config.getWorkspaceDir(), "meta.csv"));
        CSVParser metaRecords = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        if (nonNull(metaRecords)) {
            for (CSVRecord record : metaRecords) {
                // Make sample start/end rowKey.
                final String sampleStartDateString = formatDate(
                        getOffsetDate(fakeStartDate, config.getSampleLastDatePattern(), -config.getSampleLastDateAmount()), // MARK1<->MARK2
                        rowKeyDatePattern);
                final String sampleEndDateString = formatDate(
                        getOffsetDate(fakeEndDate, config.getSampleLastDatePattern(), -config.getSampleLastDateAmount()),
                        rowKeyDatePattern);
                final String sampleStartRowKey = config.getRowKey().to(safeMap(record.toMap()), sampleStartDateString);
                final String sampleEndRowKey = config.getRowKey().to(safeMap(record.toMap()), sampleEndDateString);

                // Execution
                log.info("Processing meta of sampleStartRowKey: {}, sampleEndRowKey: {}, record : {}", sampleStartRowKey,
                        sampleEndRowKey, record);
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

    protected String generateFakeRowKey(final String sampleRowKey) throws ParseException {
        final Map<String, String> rowKeyParts = config.getRowKey().from(sampleRowKey);
        final String rowKeyDateString = rowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);
        final Date fakeDate = getOffsetRowKeyDate(rowKeyDateString, rowKeyParts, config.getSampleLastDateAmount()); // MARK1<->MARK2
        return generateRowKey(rowKeyParts, sampleRowKey, fakeDate);
    }

    protected String generateRowKey(final Map<String, String> rowKeyParts, final String rowKey, final Date rowDate)
            throws ParseException {
        return config.getRowKey().to(rowKeyParts, formatDate(rowDate, rowKeyDatePattern));
    }

    protected Date getOffsetRowKeyDate(final String rowKeyDateString, final Map<String, String> rowKeyParts, int dateAmount)
            throws ParseException {
        Date date = parseDate(rowKeyDateString, rowKeyDatePattern);
        return getOffsetDate(date, config.getSampleLastDatePattern(), dateAmount);
    }

    protected Date getOffsetDate(Date date, String datePattern, int dateAmount) throws ParseException {
        switch (datePattern) {
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

    // Save to HBase table.
    protected void writeToHTable(Map<String, Object> newRecord) {
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

                // Save redo SQL to log files.
                writeRedoSqlLog(newRecord, upsertSql.toString());

                // Save undo SQL to log files.
                writeUndoSqlLog(newRecord);
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

    protected void writeUndoSqlLog(Map<String, Object> newRecord) {
        try {
            String newRowKey = (String) newRecord.get(config.getRowKey().getName());
            SqlLogFileWriter undoWriter = obtainSqlLogFileWriter(newRowKey);

            String undoSql = format("delete from \"%s\".\"%s\" where \"%s\"='%s';", config.getTableNamespace(),
                    config.getTableName(), config.getRowKey().getName(), newRowKey);
            log.debug("Undo sql: {}", undoSql);

            undoWriter.getUndoSqlWriter().append(undoSql);
            undoWriter.getUndoSqlWriter().newLine();

            final long now = currentTimeMillis();
            if (undoWriter.getBuffers().incrementAndGet() % config.getWriteSqlLogFileFlushOnBatch() == 0
                    || ((now - undoWriter.getLastFlushTime()) >= config.getWriteSqlLogFlushOnSeconds())) {
                undoWriter.getUndoSqlWriter().flush();
                undoWriter.setLastFlushTime(now);
            }
        } catch (Exception e) {
            if (config.isErrorContinue()) {
                log.warn(format("Unable write to undo sql of : %s", newRecord), e);
            } else {
                throw new IllegalStateException(e);
            }
        }
    }

    protected void writeRedoSqlLog(Map<String, Object> newRecord, String redoSql) {
        try {
            String newRowKey = (String) newRecord.get(config.getRowKey().getName());
            SqlLogFileWriter redoWriter = obtainSqlLogFileWriter(newRowKey);
            log.debug("Redo sql: {}", redoSql);

            redoWriter.getRedoSqlWriter().append(redoSql.concat(";"));
            redoWriter.getRedoSqlWriter().newLine();

            final long now = currentTimeMillis();
            if (redoWriter.getBuffers().incrementAndGet() % config.getWriteSqlLogFileFlushOnBatch() == 0
                    || ((now - redoWriter.getLastFlushTime()) >= config.getWriteSqlLogFlushOnSeconds())) {
                redoWriter.getRedoSqlWriter().flush();
                redoWriter.setLastFlushTime(now);
            }
        } catch (Exception e) {
            if (config.isErrorContinue()) {
                log.warn(format("Unable write to undo sql of : %s", newRecord), e);
            } else {
                throw new IllegalStateException(e);
            }
        }
    }

    private SqlLogFileWriter obtainSqlLogFileWriter(String rowKey) throws IOException {
        final Map<String, String> sampleStartRowKeyParts = config.getRowKey().from(rowKey);
        final String undoSqlKey = safeMap(sampleStartRowKeyParts).entrySet()
                .stream()
                .filter(e -> !eqIgnCase(e.getKey(), RowKeySpec.DATE_PATTERN_KEY))
                .map(e -> e.getValue())
                .collect(Collectors.joining("-"));

        SqlLogFileWriter sqlLogWriter = sqlLogFileWriters.get(undoSqlKey);
        if (isNull(sqlLogWriter)) {
            synchronized (this) {
                sqlLogWriter = sqlLogFileWriters.get(undoSqlKey);
                if (isNull(sqlLogWriter)) {
                    final BufferedWriter undoSqlWriter = new BufferedWriter(
                            new FileWriter(new File(config.getUndoSqlDir(), undoSqlKey.concat(".sql"))));
                    final BufferedWriter redoSqlWriter = new BufferedWriter(
                            new FileWriter(new File(config.getRedoSqlDir(), undoSqlKey.concat(".sql"))));
                    sqlLogFileWriters.put(undoSqlKey,
                            sqlLogWriter = new SqlLogFileWriter(undoSqlWriter, redoSqlWriter, new AtomicLong(0), 0L));
                }
            }
        }

        return sqlLogWriter;
    }

    public static enum FakeProvider {
        SIMPLE, MONOTONE_INCREASE;
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class SqlLogFileWriter {
        private BufferedWriter undoSqlWriter;
        private BufferedWriter redoSqlWriter;
        private AtomicLong buffers;
        private long lastFlushTime;
    }

}
