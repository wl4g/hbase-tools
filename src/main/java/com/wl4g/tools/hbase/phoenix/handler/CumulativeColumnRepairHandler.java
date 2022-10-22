package com.wl4g.tools.hbase.phoenix.handler;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Double.parseDouble;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addHours;
import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addMonths;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.addYears;

import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wl4g.infra.common.lang.DateUtils2;
import com.wl4g.tools.hbase.phoenix.config.PhoenixRepairProperties;
import com.wl4g.tools.hbase.phoenix.util.RandomOffsetUtil;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;
import com.wl4g.tools.hbase.phoenix.util.VariableParseUtil;
import com.wl4g.tools.hbase.phoenix.util.VariableParseUtil.VariableInfo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link CumulativeColumnRepairHandler}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public class CumulativeColumnRepairHandler implements InitializingBean, ApplicationRunner {

    private @Autowired PhoenixRepairProperties config;
    private @Autowired JdbcTemplate jdbcTemplate;
    private final AtomicInteger totalOfAll = new AtomicInteger(0); // e.g:devices-total
    private final AtomicInteger completedOfAll = new AtomicInteger(0);
    private ExecutorService executor;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.executor = Executors.newFixedThreadPool((config.getThreadPools() <= 1) ? 1 : config.getThreadPools());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LinkedHashMap<String, VariableInfo> variables = VariableParseUtil.parseVariables(config.getRowKey().getFormat());
        log.info("Parsed rowKey variables : {}", variables);

        // see: https://www.baeldung.com/apache-commons-csv
        log.info("Loading metadata from csv ...");

        Reader in = new FileReader(config.getMetaCsvFile());
        CSVParser records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        if (nonNull(records)) {
            for (CSVRecord record : records) {
                log.info("Processing meta record : {}", record);
                String fetchStartRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getStartDate());
                String fetchEndRowKey = config.getRowKey().to(safeMap(record.toMap()), config.getEndDate());
                executor.submit(new ProcessTask(fetchStartRowKey, fetchEndRowKey));
            }
            log.info("Waiting for running completion with {}sec ...", config.getAwaitSeconds());
            executor.awaitTermination(config.getAwaitSeconds(), TimeUnit.SECONDS);

            log.info("Processed all completed of {}/{}", completedOfAll.get(), totalOfAll.get());
            executor.shutdown();
        }
    }

    @AllArgsConstructor
    class ProcessTask implements Runnable {
        private String fetchStartRowKey;
        private String fetchEndRowKey;
        private final AtomicInteger completed = new AtomicInteger(0);

        @Override
        public void run() {
            // Load history raw records.
            // e.g: 11111111,ELE_P,111,08,20170729165254063
            String queryRawSql = format("select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' limit %s",
                    config.getTableNamespace(), config.getTableName(), fetchStartRowKey, fetchEndRowKey, config.getMaxLimit());

            log.info("Fetching: {}", queryRawSql);
            final List<Map<String, Object>> fromRecords = jdbcTemplate.queryForList(queryRawSql);

            // Transform generate new records.
            safeList(fromRecords).parallelStream().map(record -> {
                String queryOffsetSql = getQueryOffsetSql(record);
                log.debug("Fetching offset: {}", queryOffsetSql);
                List<Map<String, Object>> result = jdbcTemplate.queryForList(queryOffsetSql);

                Map<String, Object> newRecord = new HashMap<>();
                safeMap(record).forEach((columName, value) -> {
                    if (eqIgnCase(columName, config.getRowKey().getName())) {
                        String newRowKey = generateNewRowKey(fetchStartRowKey, fetchEndRowKey, (String) value);
                        newRecord.put(columName, newRowKey);
                    } else {
                        if (((value instanceof String) && isNumeric((String) value))) {
                            double _value = RandomOffsetUtil.random(config.getOffset().getValueOffsetWithRandomMinPercentage(),
                                    config.getOffset().getValueOffsetWithRandomMaxPercentage(), parseDouble((String) value));
                            newRecord.put(columName, _value);
                        } else {
                            newRecord.put(columName, value);
                        }
                    }
                    totalOfAll.incrementAndGet();
                });
                return newRecord;
            }).forEach(newRecord -> {
                // Save to HBase to table.
                try {
                    StringBuilder upsertSql = new StringBuilder(
                            format("upsert into \"%s\".\"%s\" (", config.getTableNamespace(), config.getTableName()));
                    safeMap(newRecord).forEach((columName, value) -> {
                        upsertSql.append("\"");
                        upsertSql.append(columName);
                        upsertSql.append("\",");
                    });
                    upsertSql.delete(upsertSql.length() - 1, upsertSql.length());
                    upsertSql.append(") values (");
                    safeMap(newRecord).forEach((columName, value) -> {
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

                    completed.incrementAndGet();
                    completedOfAll.incrementAndGet();
                } catch (Exception e) {
                    log.warn(format("Unable to write to htable of : %s", newRecord), e);
                }
            });

            log.info("Processed completed of {}/{}", completed.get(), fromRecords.size());
        }
    }

    /**
     * for example:
     * 
     * <pre>
     * select (max(to_number("activePower"))-min(to_number("activePower"))) activePower,(max(to_number("reactivePower"))-min(to_number("reactivePower"))) reactivePower
     * from "safeclound"."tb_ammeter" where "ROW">='11111277,ELE_P,134,01,2022102121' and "ROW"<='11111277,ELE_P,134,01,202210212221' limit 10;
     * </pre>
     */
    String getQueryOffsetSql(Map<String, Object> record) {
        String rowKey = (String) record.get(config.getRowKey().getName());

        StringBuilder queryOffsetSqlColumParts = new StringBuilder("select ");
        for (String columnName : safeList(config.getCumulative().getColumnNames())) {
            queryOffsetSqlColumParts.append("(max(to_number(\"");
            queryOffsetSqlColumParts.append(columnName);
            queryOffsetSqlColumParts.append("\"))-min(to_number(\"");
            queryOffsetSqlColumParts.append("\"))) ");
            queryOffsetSqlColumParts.append(columnName);
            queryOffsetSqlColumParts.append(",");
        }
        queryOffsetSqlColumParts.delete(queryOffsetSqlColumParts.length() - 1, queryOffsetSqlColumParts.length());

        return format("select %s from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s'", queryOffsetSqlColumParts,
                config.getTableNamespace(), config.getTableName(), "", "");
    }

    String generateNewRowKey(String fetchStartRowKey, String fetchEndRowKey, String rowKey) {
        final Map<String, String> parts = config.getRowKey().from(fetchStartRowKey);

        // Extract date from rowKey.
        String rowKeyDatePattern = config.getRowKey().ensureInit().getVariables().get(RowKeySpec.DATE_PATTERN_KEY).getName();
        String dateString = parts.get(RowKeySpec.DATE_PATTERN_KEY);
        Date date = null;
        try {
            date = DateUtils2.parseDate(dateString, rowKeyDatePattern);
            switch (config.getOffset().getRowKeyOffsetDatePattern()) {
            case "yy":
            case "y":
            case "YY":
            case "Y":
                date = addYears(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            case "MM":
            case "M":
                date = addMonths(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            case "dd":
            case "d":
            case "DD":
            case "D":
                date = addDays(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            case "HH":
            case "H":
            case "hh":
            case "h":
                date = addHours(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            case "mm":
            case "m":
                date = addMinutes(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            case "ss":
            case "s":
                date = addSeconds(date, config.getOffset().getRowKeyOffsetDateAmount());
                break;
            }
        } catch (ParseException e) {
            if (config.isErrorContinue()) {
                log.warn(format("Could not to offset parse for %s. - %s", rowKey, e.getMessage()));
            } else {
                throw new IllegalArgumentException(e);
            }
        }

        return config.getRowKey().to(parts, formatDate(date, rowKeyDatePattern));
    }

}
