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
    public void execute() throws Exception {
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
    @Override
    protected void executeUpdateToHTable(Map<String, Object> deleteRecord) {
        final String deleteRowKey = (String) deleteRecord.get(config.getRowKey().getName());
        final String deleteSql = format("delete from \"%s\".\"%s\" where \"%s\"='%s'", config.getTableNamespace(),
                config.getTableName(), config.getRowKey().getName(), deleteRowKey);

        log.info("Executing: {}", deleteSql);
        if (!config.isDryRun()) {
            jdbcTemplate.execute(deleteSql.toString());

            // Save redo SQL to log files.
            writeRedoSqlLog(deleteRecord, deleteSql.toString());

            // Save undo SQL to log files.
            writeUndoSqlLog(deleteRecord);
        }
        completedOfAll.incrementAndGet();
    }

    @Override
    protected void writeUndoSqlLog(Map<String, Object> record) {
        String deleteRowKey = (String) record.get(config.getRowKey().getName());
        doWriteSqlLog(() -> deleteRowKey, () -> {
            StringBuilder undoSql = new StringBuilder(
                    format("upsert into \"%s\".\"%s\" (", config.getTableNamespace(), config.getTableName()));
            safeMap(record).forEach((columnName, value) -> {
                undoSql.append("\"");
                undoSql.append(columnName);
                undoSql.append("\",");
            });
            undoSql.delete(undoSql.length() - 1, undoSql.length());
            undoSql.append(") values (");
            safeMap(record).forEach((columnName, value) -> {
                String symbol = "'";
                if (nonNull(value) && value instanceof Number) {
                    symbol = "";
                }
                undoSql.append(symbol);
                undoSql.append(value);
                undoSql.append(symbol);
                undoSql.append(",");
            });
            undoSql.delete(undoSql.length() - 1, undoSql.length());
            undoSql.append(")");
            return undoSql.toString();
        });
    }

}
