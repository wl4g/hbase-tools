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

### Execution fake (Generate random floating simulation data based on Htable historical data)

```bash
java -jar phoenix-fake-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 \
--fake.tableNamespace=safeclound \
--fake.tableName=tb_ammeter \
--fake.dryRun=true \
--fake.threadPools=1 \
--fake.maxLimit=14400 \
--fake.errorContinue=false \
--fake.awaitSeconds=1800 \
--fake.rowKey.name=ROW \
--fake.rowKey.template="{text:addrIP:},ELE_P,{text:templateMark:},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}" \
--fake.sample.startDate=202210202114 \
--fake.sample.endDate=202210212114 \
--fake.sample.lastDateAmount=-7 \
--fake.generator.rowKeyDatePattern=dd \
--fake.generator.rowKeyDateAmcount=1 \
--fake.generator.valueRandomMinPercent=0.8976 \
--fake.generator.valueRandomMaxPercent=1.1024 \
--fake.cumulative.columnNames[0]=activePower \
--fake.cumulative.columnNames[1]=reactivePower \
| grep upsert | awk -F ' ' '{print $15}' | awk -F "'" '{print $4}' | sed s/11111277,ELE_P,134,01,//g
```

- Filter real-time log tricks examples:

```bash
# Filter the rowKey date that generated fake data records 
java -jar phoenix-fake-1.0.0-bin.jar | grep upsert | awk -F ' ' '{print $15}' | awk -F "'" '{print $4}' | sed s/11111277,ELE_P,134,01,//g

# Filter processed statistics.
java -jar phoenix-fake-1.0.0-bin.jar | grep Processed
```

- ***Notice:*** The parameter: `--fake.rowKey.template` template generated for HBase table rowKey value, which supports types: `text`, `date`, template specification such as: `{text:myname1:myformat1}mydelimiter1{date:yyyyMMddHHmmssSSS}mydelimiter2{text:myname2:myformat2}...`, features refer to: [RowKeySpecTests](src/test/java/com/wl4g/tools/hbase/phoenix/util/RowKeySpecTests.java)


## Developer guide

- Building

```bash
git clone git@github.com/wl4g/phoenix-fake-tool.git
mvn clean package -DskipTests -Pphoenix4 -Pbuild:springjar
```
