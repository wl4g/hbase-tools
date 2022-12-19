package com.wl4g.tools.hbase.phoenix.modules.imports;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static java.lang.String.format;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.wl4g.infra.common.io.FileIOUtils;
import com.wl4g.tools.hbase.phoenix.config.ToolsProperties.RunnerProvider;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link SimplePhoenixTableImporter}
 * 
 * @author James Wong
 * @version 2022-12-08
 * @since v1.0.0
 */
@Slf4j
public class SimplePhoenixTableImporter extends PhoenixTableImporter {

    @Override
    protected RunnerProvider provider() {
        return RunnerProvider.SIMPLE_IMPORTER;
    }

    @Override
    protected Runnable newProcessTask(List<File> importSqlFile) {
        return new ProcessTask(importSqlFile);
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private List<File> importSqlFiles;
        private final AtomicInteger completed = new AtomicInteger(0);

        @Override
        public void run() {
            try {
                for (File file : safeList(importSqlFiles)) {
                    int startIndex = 0;
                    List<String> lines = null;
                    while (!(lines = FileIOUtils.readLines(file.getAbsolutePath(), startIndex, 1024)).isEmpty()) {
                        startIndex += lines.size();
                        safeList(lines).parallelStream().forEach(line -> {
                            final String upsertSql = line.substring(0, line.lastIndexOf(";"));
                            try {
                                totalOfAll.incrementAndGet();
                                log.info("Executing: {}", upsertSql);
                                if (!config.isDryRun()) {
                                    jdbcTemplate.execute(upsertSql);
                                    // TODO parse sql to ROW value
                                    // writeUndoSqlLog(null);
                                }
                                completed.incrementAndGet();
                            } catch (Exception e2) {
                                log.error(format("Failed to process of upsertSql: %s", upsertSql), e2);
                                if (!config.isErrorContinue()) {
                                    throw new IllegalStateException(e2);
                                }
                            }
                        });
                    }
                }
                log.info("Processed completed of {}/{}/{}/{}", completed.get(), importSqlFiles.size(), completedOfAll.get(),
                        totalOfAll.get());
            } catch (Exception e2) {
                log.error(format("Failed to process of importSqlFiles: %s", importSqlFiles), e2);
                if (!config.isErrorContinue()) {
                    throw new IllegalStateException(e2);
                }
            }
        }

    }

}
