package com.wl4g.tools.hbase.phoenix.cleanup;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;

import com.wl4g.tools.hbase.phoenix.BaseToolRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract cleaner records handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class PhoenixTableCleaner extends BaseToolRunner {

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
                // Make delete start/end rowKey.
                final String deleteStartDateString = formatDate(targetStartDate, rowKeyDatePattern);// MARK1<->MARK2
                final String deleteEndDateString = formatDate(targetEndDate, rowKeyDatePattern);

                final String deleteStartRowKey = config.getRowKey().to(safeMap(record.toMap()), deleteStartDateString);
                final String deleteEndRowKey = config.getRowKey().to(safeMap(record.toMap()), deleteEndDateString);

                // Execution
                log.info("Processing meta of deleteStartRowKey: {}, deleteEndRowKey: {}, record : {}", deleteStartRowKey,
                        deleteEndRowKey, record);
                executor.submit(newProcessTask(deleteStartRowKey, deleteEndRowKey));
            }
            destroy();
        }
    }

    protected abstract Runnable newProcessTask(String deleteStartRowKey, String deleteEndRowKey);

    // Delete update to HBase table.
    protected void deleteToHTable(Map<String, Object> newRecord) {
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
            String symbol = "'";
            if (nonNull(value) && value instanceof Number) {
                symbol = "";
            }
            upsertSql.append(symbol);
            upsertSql.append(value);
            upsertSql.append(symbol);
            upsertSql.append(",");
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
    }

}
