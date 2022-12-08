# Phoenix Tools

## Quick start (example):

### Initial example data:

- [mysql-init.sql](testdata/mysql-init.sql)

- [phoenix-init.sql](testdata/phoenix-init.sql)

### Generate meta CSV SQLs

- for example:

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
    INNER JOIN ed_equiptemplate t ON t.templateId = e.templateId 
WHERE
    e.workingStatus = 1
    AND e.`status` = 0
    AND c.isEnabled = 0
    AND t.`status` = 0
    AND c.customerName LIKE '%佛山市xxx印染%'
    AND e.addrIP IS NOT NULL 
    AND e.addrIPOrder > 0
```

### Startup runner

- [Generating missing data with Faker](./README_FAKER.md)

- [Removing useless data with Cleaner](./README_CLEANER.md)

## Developer guide

- Building

```bash
git clone git@github.com/wl4g/phoenix-tools.git
mvn clean package -DskipTests -Pphoenix4 -Pbuild:springjar
```

## FAQ

- How the generating GraalVM `relfect-config.json`. Notice: The success passed tested versions are: `graalvm-ce-java8-21.0.0.2`, and the failed versions are: `graalvm-ce-java11-22.1.0`

```bash
/usr/local/graalvm-ce-java8-21.0.0.2/bin/java -jar \
-agentlib:native-image-agent=config-merge-dir=/tmp/configdir/ \
phoenix-tools-1.0.0-bin.jar \
--spring.datasource.url=jdbc:phoenix:localhost:2181 ...
```
