# Phoenix Fake Tool

## Quick start

- Generate (principals/equipments)meta CSV example:

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
    c.customerName LIKE '%粤樵东%' 
    AND e.addrIP IS NOT NULL 
    AND e.addrIPOrder > 0
```

- Execution fix tools (generated random offset record to HTable based on history records)

```bash
java -jar phoenix-repair-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 \
--repair.tableNamespace=safeclound \
--repair.tableName=tb_ammeter \
--repair.rowKeyFormat="{csv:addrIP:},ELE_P,{csv:templateMark:},{csv:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}" \
--repair.startDate=2022102121 \
--repair.endDate=202210212221 \
--repair.rowKeyOffsetDatePattern=dd \
--repair.rowKeyOffsetDateAmcount=1 \
--repair.valueOffsetWithRandomMinPercentage=0.8976 \
--repair.valueOffsetWithRandomMaxPercentage=1.1024 \
--repair.threadPools=1 \
--repair.awaitSeconds=1800 \
--repair.errorContinue=false \
--repair.dryRun=true
```
