package com.wl4g.tools.hbase.phoenix.cleanup;

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

import com.wl4g.tools.hbase.phoenix.BaseToolRunner;
import com.wl4g.tools.hbase.phoenix.config.CleanerProperties;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
                // Make sample start/end rowKey.
                final String sampleStartDateString = formatDate(
                        getOffsetDate(cleanerStartDate, config.getSampleLastDatePattern(), -config.getSampleLastDateAmount()), // MARK1<->MARK2
                        rowKeyDatePattern);
                final String sampleEndDateString = formatDate(
                        getOffsetDate(cleanerEndDate, config.getSampleLastDatePattern(), -config.getSampleLastDateAmount()),
                        rowKeyDatePattern);
                final String sampleStartRowKey = config.getRowKey().to(safeMap(record.toMap()), sampleStartDateString);
                final String sampleEndRowKey = config.getRowKey().to(safeMap(record.toMap()), sampleEndDateString);

                // Execution
                log.info("Processing meta of sampleStartRowKey: {}, sampleEndRowKey: {}, record : {}", sampleStartRowKey,
                        sampleEndRowKey, record);
                executor.submit(newProcessTask(sampleStartRowKey, sampleEndRowKey));
            }
            destroy();
        }
    }

    protected abstract Runnable newProcessTask(String sampleStartRowKey, String sampleEndRowKey);

}
