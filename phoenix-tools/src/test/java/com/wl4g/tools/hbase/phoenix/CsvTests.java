/*
 * Copyright 2017 ~ 2025 the original author or authors. James Wong <jameswong1376@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.tools.hbase.phoenix;

import static org.apache.commons.lang3.SystemUtils.USER_HOME;

import java.io.FileReader;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

/**
 * {@link CsvTests}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v3.0.0
 */
public class CsvTests {

    @Test
    public void testReadCsv() throws Exception {
        Reader in = new FileReader(USER_HOME + "/customer_equipments_query.csv");
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().withSkipHeaderRecord().parse(in);
        for (CSVRecord record : records) {
            // System.out.println(record.get(0)); // first column
            System.out.println(record.toMap());
        }
    }

}
