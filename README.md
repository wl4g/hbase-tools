# Phoenix Fake Tool

## Quick start (example):

### Initial example data:

- [mysql-init.sql](testdata/mysql-init.sql)

- [phoenix-init.sql](testdata/phoenix-init.sql)

### Generate meta CSV SQLs example:

```bash
SELECT
    c.customerName,
    e.equipmentName,
    e.addrIP,
    e.addrIPOrder,
    t.templateMark
FROM
    ed_equipmentinfo e
    INNER JOIN ed_customer c ON c.customerId = e.customerId
    LEFT JOIN ed_equiptemplate t ON t.templateId = e.templateId 
WHERE
    c.customerName LIKE '%佛山市xxx印染%' 
    AND e.addrIP IS NOT NULL 
    AND e.addrIPOrder > 0
```

### Execution faker (Generate fake data based on characteristics such as historical sample values)

```bash
java -jar phoenix-fake-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 \
--fake.workspaceDir=${HOME}/.phoenix-fake-tool/ \
--fake.undoSQLStageFlushOnCount=1024 \
--fake.undoSQLStageFlushOnSeconds=2 \
--fake.tableNamespace=safeclound \
--fake.tableName=tb_ammeter \
--fake.dryRun=true \
--fake.threadPools=1 \
--fake.maxLimit=14400 \
--fake.errorContinue=false \
--fake.awaitSeconds=1800 \
--fake.rowKey.name=ROW \
--fake.rowKey.template="{text:addrIP:},ELE_P,{text:templateMark:},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}" \
--fake.startDate=202210212114 \
--fake.endDate=202210220835 \
--fake.sampleLastDatePattern=dd \
--fake.sampleLastDateAmount=1 \
--fake.valueMinRandomPercent=1.0124 \
--fake.valueMaxRandomPercent=1.0987 \
--fake.columnNames[0]=activePower \
--fake.columnNames[1]=reactivePower \
--fake.monotoneIncrease.sampleBeforeAverageDateAmount=3 \
--fake.provider=MONOTONE_INCREASE
```

- Filter real-time log tricks examples:

```bash
# Filter the rowKey date that generated fake data records 
java -jar phoenix-fake-1.0.0-bin.jar | grep upsert | awk -F ' ' '{print $15}' | awk -F "'" '{print $4}' | sed s/11111277,ELE_P,134,01,//g

# Filter processed statistics.
java -jar phoenix-fake-1.0.0-bin.jar | grep Processed
```

## Configuration

- `--fake.workspaceDir`: The directory of the workspace, default is: `${HOME}/.phoenix-fake-tool/`. Default metadata file: `{workspaceDir}/meta.csv`, undo SQL dir: `{workspaceDir}/undo-{timestamp}/`

- `--fake.undoSQLStageFlushOnBatch`: How many batch count to undo buffered writes to SQL to disk every. default is: `1024`.

- `--fake.undoSQLStageFlushOnSeconds`: How many seconds to undo buffered writes to SQL to disk every. default is: `2`.

- `--fake.dryRun`: The specifies whether it is a test run mode, that is, it will not actually write to the Phoenix table, the default is: `true`.

- `--fake.rowKey.template`: The template generated for write to HBase table rowKey value, which supports types: `text`, `date`, template specification such as: `{text:myname1:myformat1}mydelimiter1{date:yyyyMMddHHmmssSSS}mydelimiter2{text:myname2:myformat2}...`, features refer to: [RowKeySpecTests](src/test/java/com/wl4g/tools/hbase/phoenix/util/RowKeySpecTests.java)

- `--fake.startDate|endEndDate`: The start/end date to generate fake data, e.g: `202210212114/202210220835`

- `--fake.sampleLastDatePattern`: When generating fake data, it is necessary to refer to the data samples of the previous time period as the material of rowKey, and this is the scale pattern of the time period. default is: `dd`

- `--fake.sampleLastDateAmount`: See config: `--fake.sampleLastDatePattern`, the represents the amount of date. default is: `1` 

- `--fake.valueMinRandomPercent|valueMaxRandomPercent`: When using Cumulative Fake (i.e. incrementing) to generate fake data, the minimum and maximum random percentages should be `>1`, conversely, if the generate fake data does not need to be incremented, the minimum random percentage can be `<1`.

- `--fake.provider=MONOTONE_INCREASE|SIMPLE`: The setup fake provider, The `MONOTONE_INCREASE` algorithm is
based on the average value of the first `--fake.monotoneIncrease.sampleBeforeAverageDateAmount` cycles multiplied by a random factor, and then accumulated, default is: `3`; `SIMPLE` provider calculation is historical value * random number.

## Developer guide

- Building

```bash
git clone git@github.com/wl4g/phoenix-fake-tool.git
mvn clean package -DskipTests -Pphoenix4 -Pbuild:springjar
```
