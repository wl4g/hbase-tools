package com.wl4g.tools.hbase.phoenix;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.time.DateUtils.parseDate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wl4g.tools.hbase.phoenix.config.ToolsProperties;
import com.wl4g.tools.hbase.phoenix.config.ToolsProperties.RunnerProvider;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract based tools handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class BaseToolRunner implements InitializingBean, DisposableBean, ApplicationRunner {

    protected @Autowired ToolsProperties config;
    protected @Autowired JdbcTemplate jdbcTemplate;
    protected final AtomicInteger totalOfAll = new AtomicInteger(0); // e.g:devices-total
    protected final AtomicInteger completedOfAll = new AtomicInteger(0);
    protected ExecutorService executor;
    protected String rowKeyDatePattern;
    protected Map<String, SqlLogFileWriter> sqlLogFileWriters = new ConcurrentHashMap<>(1024);
    protected Date targetStartDate;
    protected Date targetEndDate;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!isActive()) {
            return;
        }
        final String prefix = getClass().getSimpleName();
        final AtomicInteger counter = new AtomicInteger(0);
        this.executor = Executors.newFixedThreadPool((config.getThreadPools() <= 1) ? 1 : config.getThreadPools(),
                r -> new Thread(r, prefix.concat("-" + counter.incrementAndGet())));

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
        this.targetStartDate = parseDate(startDate += complement, rowKeyDatePattern);
        this.targetEndDate = parseDate(endDate += complement, rowKeyDatePattern);
    }

    @Override
    public void destroy() throws Exception {
        if (!isActive()) {
            return;
        }

        if (!executor.isShutdown()) {
            executor.shutdown();
            log.info("Waiting for all tasks to complete, timeout is {}sec ...", config.getAwaitSeconds());
            int count = 0, total = (int) (config.getAwaitSeconds() * 10); // 控制超时时强制结束
            boolean completed = false; // 控制所有执行成功时立即结束
            while (++count < total && !completed) {
                completed = executor.awaitTermination(100, TimeUnit.MILLISECONDS);
            }
            if (!completed) {
                // see:https://blog.csdn.net/BIT_666/article/details/125180030
                // 如果所有任务在关闭后都已完成，则返回true，请注意，除非首先调用了shutdown或shutdownNow，否则isTerminated永远为false。
                // executor.isTerminated();
                //
                // 优雅停止接收新task，正在运行的task继续运行，立即返回
                // executor.shutdown();
                //
                // 立即停止接收新task，终止正在运行的task，立即返回正在等待执行的tasks(注:不是正在运行的tasks).
                List<Runnable> paddingRunTasks = executor.shutdownNow();
                log.info("Some processed completed successful of {}/{}, padding tasks: {}", completedOfAll.get(),
                        totalOfAll.get(), paddingRunTasks);
            } else {
                log.info("Successfully processed all completed of {}/{}", completedOfAll.get(), totalOfAll.get());
            }
        }

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
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isActive()) {
            log.warn("No run execute, {} is inactive !", provider());
            return;
        }
        execute();
    }

    protected abstract void execute() throws Exception;

    protected abstract RunnerProvider provider();

    protected boolean isActive() {
        return provider() == config.getProvider();
    }

    protected List<Map<String, Object>> fetchRecords(String startRowKey, String endRowKey) {
        // e.g: 11111111,ELE_P,111,08,20170729165254063
        String queryRawSql = format(
                "select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' order by \"ROW\" asc limit %s",
                config.getTableNamespace(), config.getTableName(), startRowKey, endRowKey, config.getMaxLimit());
        log.info("Fetching: {}", queryRawSql);
        return safeList(jdbcTemplate.queryForList(queryRawSql));
    }

    protected abstract void executeUpdateToHTable(Map<String, Object> record);

    protected void writeRedoSqlLog(Map<String, Object> record, String redoSql) {
        String newRowKey = (String) record.get(config.getRowKey().getName());
        doWriteSqlLog(true, () -> newRowKey, () -> redoSql);
    }

    protected abstract void writeUndoSqlLog(Map<String, Object> record);

    protected void doWriteSqlLog(boolean isRedoSqlLog, Callable<String> rowKeyCall, Callable<String> sqlCall) {
        String rowKey = null;
        String sql = null;
        try {
            rowKey = rowKeyCall.call();
            sql = sqlCall.call();
            hasTextOf(rowKey, "rowKey");
            hasTextOf(sql, "sql");

            SqlLogFileWriter redoWriter = obtainSqlLogFileWriter(rowKey);
            log.debug("write sql: {}", sql);

            synchronized (this) {
                if (isRedoSqlLog) {
                    redoWriter.getRedoSqlWriter().append(sql.concat(";"));
                    redoWriter.getRedoSqlWriter().newLine();
                } else {
                    redoWriter.getUndoSqlWriter().append(sql.concat(";"));
                    redoWriter.getUndoSqlWriter().newLine();
                }
            }

            final long now = currentTimeMillis();
            if (redoWriter.getBuffers().incrementAndGet() % config.getWriteSqlLogFileFlushOnBatch() == 0
                    || ((now - redoWriter.getLastFlushTime()) >= config.getWriteSqlLogFlushOnMillis())) {
                if (isRedoSqlLog) {
                    redoWriter.getRedoSqlWriter().flush();
                } else {
                    redoWriter.getUndoSqlWriter().flush();
                }
                redoWriter.setLastFlushTime(now);
            }
        } catch (Exception e) {
            if (config.isErrorContinue()) {
                log.warn(format("Unable write to redo/undo sql of : %s", rowKey), e);
            } else {
                throw new IllegalStateException(e);
            }
        }
    }

    protected SqlLogFileWriter obtainSqlLogFileWriter(String rowKey) throws IOException {
        final Map<String, String> rowKeyParts = config.getRowKey().from(rowKey);
        final String sqlLogKey = safeMap(rowKeyParts).entrySet()
                .stream()
                .filter(e -> !eqIgnCase(e.getKey(), RowKeySpec.DATE_PATTERN_KEY))
                .map(e -> e.getValue())
                .collect(Collectors.joining("-"));

        SqlLogFileWriter sqlLogWriter = sqlLogFileWriters.get(sqlLogKey);
        if (isNull(sqlLogWriter)) {
            synchronized (this) {
                sqlLogWriter = sqlLogFileWriters.get(sqlLogKey);
                if (isNull(sqlLogWriter)) {
                    final BufferedWriter undoSqlWriter = new BufferedWriter(
                            new FileWriter(new File(config.getUndoSqlDir(), sqlLogKey.concat(".sql"))));
                    final BufferedWriter redoSqlWriter = new BufferedWriter(
                            new FileWriter(new File(config.getRedoSqlDir(), sqlLogKey.concat(".sql"))));
                    sqlLogFileWriters.put(sqlLogKey,
                            sqlLogWriter = new SqlLogFileWriter(undoSqlWriter, redoSqlWriter, new AtomicLong(0), 0L));
                }
            }
        }

        return sqlLogWriter;
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
