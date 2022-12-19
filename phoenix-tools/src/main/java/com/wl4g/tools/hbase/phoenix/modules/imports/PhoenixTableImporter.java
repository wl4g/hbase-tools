package com.wl4g.tools.hbase.phoenix.modules.imports;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeArrayToList;
import static java.lang.String.format;
import static java.util.Objects.nonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wl4g.tools.hbase.phoenix.BaseToolRunner;

import lombok.extern.slf4j.Slf4j;

/**
 * Abstract importer handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class PhoenixTableImporter extends BaseToolRunner {

    @Override
    public void execute() throws Exception {
        // see: https://www.baeldung.com/apache-commons-csv
        log.info("Loading metadata from csv ...");

        final File importSqlDir = new File(config.getWorkspaceDir(), config.getImporter().getImportSqlDirName());
        if (nonNull(importSqlDir)) {
            final List<File> files = safeArrayToList(importSqlDir.listFiles());
            final int shardings = files.size() / config.getThreadPools();
            int index = 0;
            List<File> shardingImportFiles = new ArrayList<>();
            for (File importSqlFile : files) {
                shardingImportFiles.add(importSqlFile);
                if (++index % shardings == 0 || index == files.size()) {
                    log.info("Processing meta of shardingImportFiles: {}", shardingImportFiles);
                    executor.submit(newProcessTask(shardingImportFiles));
                    shardingImportFiles = new ArrayList<>();
                }
            }
            destroy();
        }
    }

    protected abstract Runnable newProcessTask(List<File> importSqlFiles);

    @Override
    protected void executeUpdate(Map<String, Object> importRecord) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeUndoSqlLog(Map<String, Object> importRecord) {
        final String deleteRowKey = (String) importRecord.get(config.getRowKey().getName());
        final String deleteSql = format("delete from \"%s\".\"%s\" where \"%s\"='%s'", config.getTableNamespace(),
                config.getTableName(), config.getRowKey().getName(), deleteRowKey);

        doWriteSqlLog(false, () -> deleteRowKey, () -> deleteSql);
    }

}
