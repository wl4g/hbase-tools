# Phoenix tools for Faker

## Quick start (example):

- Execution faker (Generate fake data based on characteristics such as historical sample values)

```bash
java -jar phoenix-tools-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 \
--tools.workspaceDir=${HOME}/.phoenix-tools/ \
--tools.provider=MONOTONE_INCREASE_FAKER \
--tools.writeSqlLogFileFlushOnBatch=1024 \
--tools.writeSqlLogFlushOnMillis=500 \
--tools.tableNamespace=safeclound \
--tools.tableName=tb_ammeter \
--tools.dryRun=true \
--tools.threadPools=1 \
--tools.maxLimit=14400 \
--tools.errorContinue=true \
--tools.awaitSeconds=1800 \
--tools.rowKey.name=ROW \
--tools.rowKey.template="{text:addrIP:},ELE_P,{text:templateMark:},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}" \
--tools.startDate=202210212114 \
--tools.endDate=202210220835 \
--tools.faker.sampleLastDatePattern=dd \
--tools.faker.sampleLastDateAmount=1 \
--tools.faker.valueMinRandomPercent=0.9876 \
--tools.faker.valueMaxRandomPercent=1.0124 \
--tools.faker.columnNames[0]=activePower \
--tools.faker.columnNames[1]=reactivePower \
--tools.faker.monotoneIncrease.sampleBeforeAverageDateAmount=3
```

- Filter real-time log tricks examples:

```bash
# Filter the rowKey date that generated fake data records 
java -jar phoenix-tool-1.0.0-bin.jar | grep upsert | awk -F ' ' '{print $15}' | awk -F "'" '{print $4}' | sed s/11111277,ELE_P,134,01,//g

# Filter processed statistics.
java -jar phoenix-tool-1.0.0-bin.jar | grep Processed
```

## Configuration

- `--tools.workspaceDir`: The directory of the workspace, default is: `${HOME}/.phoenix-tool/`. Default metadata file: `{workspaceDir}/meta.csv`, undo SQL dir: `{workspaceDir}/undo-{timestamp}/` and `{workspaceDir}/redo-{timestamp}/`

- `--tools.writeSqlLogFileFlushOnBatch`: How many batch size to undo/redo sql log file buffered writes to SQL to disk every. default is: `1024`.

- `--tools.writeSqlLogFileFlushOnSeconds`: How many millis to undo/redo sql log file buffered writes to SQL to disk every. default is: `500`.

- `--tools.dryRun`: The specifies whether it is a test run mode, that is, it will not actually write to the Phoenix table, the default is: `true`.

- `--tools.rowKey.template`: The template generated for write to HBase table rowKey value, which supports types: `text`, `date`, template specification such as: `{text:myname1:myformat1}mydelimiter1{date:yyyyMMddHHmmssSSS}mydelimiter2{text:myname2:myformat2}...`, features refer to: [RowKeySpecTests](src/test/java/com/wl4g/tools/hbase/phoenix/util/RowKeySpecTests.java)

- `--tools.provider=MONOTONE_INCREASE_FAKER|SIMPLE_FAKER`: The setup fake provider, The `MONOTONE_INCREASE_FAKER` algorithm is
based on the average value of the first `--tools.faker.monotoneIncrease.sampleBeforeAverageDateAmount` cycles multiplied by a random factor, and then accumulated, default is: `3`; `SIMPLE_FAKER` provider calculation is historical value * random number.

- `--tools.faker.startDate|endEndDate`: The start/end date to generate fake data, e.g: `202210212114/202210220835`

- `--tools.faker.sampleLastDatePattern`: When generating fake data, it is necessary to refer to the data samples of the previous time period as the material of rowKey, and this is the scale pattern of the time period. default is: `dd`

- `--tools.faker.sampleLastDateAmount`: See config: `--tools.faker.sampleLastDatePattern`, the represents the amount of date. default is: `1` 

- `--tools.faker.valueMinRandomPercent|valueMaxRandomPercent`: When using Cumulative Fake (i.e. incrementing) to generate fake data, the minimum and maximum random percentages should be `>1`, conversely, if the generate fake data does not need to be incremented, the minimum random percentage can be `<1`.
