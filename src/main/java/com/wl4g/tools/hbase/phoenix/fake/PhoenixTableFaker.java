package com.wl4g.tools.hbase.phoenix.fake;

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.time.DateUtils.parseDate;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;

import com.wl4g.tools.hbase.phoenix.BaseToolRunner;
import com.wl4g.tools.hbase.phoenix.util.DateTool;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.extern.slf4j.Slf4j;

/**
 * Base fake handler.
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 * @see https://www.baeldung.com/apache-commons-csv
 */
@Slf4j
public abstract class PhoenixTableFaker extends BaseToolRunner {

    public Date getFakeStartDate() {
        return targetStartDate;
    }

    public Date getFakeEndDate() {
        return targetEndDate;
    }

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
                        getOffsetDate(getFakeStartDate(), config.getFaker().getSampleLastDatePattern(),
                                -config.getFaker().getSampleLastDateAmount()), // MARK1<->MARK2
                        rowKeyDatePattern);
                final String sampleEndDateString = formatDate(getOffsetDate(getFakeEndDate(),
                        config.getFaker().getSampleLastDatePattern(), -config.getFaker().getSampleLastDateAmount()),
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

    protected List<Map<String, Object>> fetchSampleRecords(String sampleStartRowKey, String sampleEndRowKey) {
        // e.g: 11111111,ELE_P,111,08,20170729165254063
        String queryRawSql = format(
                "select * from \"%s\".\"%s\" where \"ROW\">='%s' and \"ROW\"<='%s' order by \"ROW\" asc limit %s",
                config.getTableNamespace(), config.getTableName(), sampleStartRowKey, sampleEndRowKey, config.getMaxLimit());
        log.info("Fetching: {}", queryRawSql);
        return safeList(jdbcTemplate.queryForList(queryRawSql));
    }

    protected String generateFakeRowKey(final String sampleRowKey) throws ParseException {
        final Map<String, String> rowKeyParts = config.getRowKey().from(sampleRowKey);
        final String rowKeyDateString = rowKeyParts.get(RowKeySpec.DATE_PATTERN_KEY);
        final Date fakeDate = getOffsetRowKeyDate(rowKeyDateString, rowKeyParts, config.getFaker().getSampleLastDateAmount()); // MARK1<->MARK2
        return generateRowKey(rowKeyParts, fakeDate);
    }

    protected String generateRowKey(final Map<String, String> rowKeyParts, final Date rowDate) throws ParseException {
        return config.getRowKey().to(rowKeyParts, formatDate(rowDate, rowKeyDatePattern));
    }

    protected Date getOffsetRowKeyDate(final String rowKeyDateString, final Map<String, String> rowKeyParts, int dateAmount)
            throws ParseException {
        Date date = parseDate(rowKeyDateString, rowKeyDatePattern);
        return getOffsetDate(date, config.getFaker().getSampleLastDatePattern(), dateAmount);
    }

    protected Date getOffsetDate(Date date, String datePattern, int dateAmount) throws ParseException {
        return DateTool.getOffsetDate(date, datePattern, dateAmount);
    }

}
