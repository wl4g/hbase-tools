# Phoenix tools for Cleaner

## Quick start

- Execution cleaner example:

```bash
java -jar phoenix-tools-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 \
--tools.workspaceDir=${HOME}/.phoenix-tools/ \
--tools.provider=SIMPLE_CLEANER \
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
--tools.endDate=202210220835
```

- Filter real-time log tricks examples:

```bash
# Filter processed statistics.
java -jar phoenix-tools-1.0.0-bin.jar | grep Processed
```

## Configuration

- TODO
