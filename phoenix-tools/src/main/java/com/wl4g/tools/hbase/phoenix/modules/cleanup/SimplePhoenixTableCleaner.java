package com.wl4g.tools.hbase.phoenix.modules.cleanup;

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
public class SimplePhoenixTableCleaner extends PhoenixTableCleaner {

    @Override
    protected RunnerProvider provider() {
        return RunnerProvider.SIMPLE_CLEANER;
    }

    @Override
    protected Runnable newProcessTask(String deleteStartRowKey, String deleteEndRowKey) {
        return new ProcessTask(deleteStartRowKey, deleteEndRowKey);
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String deleteStartRowKey;
        private String deleteEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);

        @Override
        public void run() {
            try {
                List<Map<String, Object>> deleteRecords = fetchRecords(deleteStartRowKey, deleteEndRowKey);

                safeList(deleteRecords).parallelStream().forEach(deleteRecord -> {
                    totalOfAll.incrementAndGet();
                    executeUpdate(deleteRecord);
                    completed.incrementAndGet();
                });

                log.info("Processed completed of {}/{}/{}/{}", completed.get(), deleteRecords.size(), completedOfAll.get(),
                        totalOfAll.get());
            } catch (Exception e2) {
                log.error(format("Failed to process of deleteStartRowKey: %s, deleteEndRowKey: %s", deleteStartRowKey,
                        deleteEndRowKey), e2);
                if (!config.isErrorContinue()) {
                    throw new IllegalStateException(e2);
                }
            }
        }

    }

}
