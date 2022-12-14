package com.wl4g.tools.hbase.phoenix.modules.exports;

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
 * Abstract exporter handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class PhoenixTableExporter extends BaseToolRunner {

    @Override
    public void execute() throws Exception {
        // see: https://www.baeldung.com/apache-commons-csv
        log.info("Loading metadata from csv ...");

        Reader in = new FileReader(new File(config.getWorkspaceDir(), "meta.csv"));
        CSVParser metaRecords = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        if (nonNull(metaRecords)) {
            for (CSVRecord record : metaRecords) {
                // Make export start/end rowKey.
                final String exportStartDateString = formatDate(targetStartDate, rowKeyDatePattern);// MARK1<->MARK2
                final String exportEndDateString = formatDate(targetEndDate, rowKeyDatePattern);

                final String exportStartRowKey = config.getRowKey().to(safeMap(record.toMap()), exportStartDateString);
                final String exportEndRowKey = config.getRowKey().to(safeMap(record.toMap()), exportEndDateString);

                // Execution
                log.info("Processing meta of exportStartRowKey: {}, exportEndRowKey: {}, record : {}", exportStartRowKey,
                        exportEndRowKey, record);
                executor.submit(newProcessTask(exportStartRowKey, exportEndRowKey));
            }
            destroy();
        }
    }

    protected abstract Runnable newProcessTask(String exportStartRowKey, String exportEndRowKey);

    // Delete update to HBase table.
    @Override
    protected void executeUpdate(Map<String, Object> exportRecord) {
        final StringBuilder upsertSql = new StringBuilder(
                format("upsert into \"%s\".\"%s\" (", config.getTableNamespace(), config.getTableName()));
        safeMap(exportRecord).forEach((columnName, value) -> {
            upsertSql.append("\"");
            upsertSql.append(columnName);
            upsertSql.append("\",");
        });
        upsertSql.delete(upsertSql.length() - 1, upsertSql.length());
        upsertSql.append(") values (");
        safeMap(exportRecord).forEach((columnName, value) -> {
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

        log.info("Exporting: {}", upsertSql);
        if (!config.isDryRun()) {
            // Save redo SQL to log files.
            writeRedoSqlLog(exportRecord, upsertSql.toString());
        }
        completedOfAll.incrementAndGet();
    }

}
