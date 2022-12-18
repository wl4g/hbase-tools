package com.wl4g.tools.hbase.phoenix.modules.exports;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
public class SimplePhoenixTableExporter extends PhoenixTableExporter {

    @Override
    protected RunnerProvider provider() {
        return RunnerProvider.SIMPLE_EXPORTER;
    }

    @Override
    protected Runnable newProcessTask(String exportStartRowKey, String exportEndRowKey) {
        return new ProcessTask(exportStartRowKey, exportEndRowKey);
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String exportStartRowKey;
        private String exportEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);

        @Override
        public void run() {
            try {
                final List<Map<String, Object>> exportRecords = fetchRecords(exportStartRowKey, exportEndRowKey);

                safeList(exportRecords).parallelStream().forEach(exportRecord -> {
                    totalOfAll.incrementAndGet();
                    executeUpdate(exportRecord);
                    completed.incrementAndGet();
                });

                log.info("Processed completed of {}/{}/{}/{}", completed.get(), exportRecords.size(), completedOfAll.get(),
                        totalOfAll.get());
            } catch (Exception e2) {
                log.error(format("Failed to process of exportStartRowKey: %s, exportEndRowKey: %s", exportStartRowKey,
                        exportEndRowKey), e2);
                if (!config.isErrorContinue()) {
                    throw new IllegalStateException(e2);
                }
            }
        }

    }

}
