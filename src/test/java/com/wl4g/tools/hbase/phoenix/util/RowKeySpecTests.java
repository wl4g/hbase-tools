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
package com.wl4g.tools.hbase.phoenix.util;

import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.wl4g.infra.common.lang.DateUtils2;

/**
 * {@link RowKeySpecTests}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v3.0.0
 */
public class RowKeySpecTests {

    @Test
    public void testInitWithPrefix() {
        // System.out.println(format("%02d", 1));

        RowKeySpec spec1 = RowKeySpec.builder()
                .template("myprefix{text:addrIP},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}")
                .build();
        spec1.ensureInit();

        spec1.getVariables().forEach((k, v) -> System.out.println(k + " => " + v));
        System.out.println();

        spec1.getDelimiters().forEach(delimiter -> System.out.println("delimiter => " + delimiter));
        Assertions.assertEquals("myprefix", spec1.getDelimiters().get(0));
    }

    @Test
    public void testInitWithNonPrefix() {
        RowKeySpec spec2 = RowKeySpec.builder()
                .template("{text:addrIP},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}")
                .build();
        spec2.ensureInit();

        spec2.getVariables().forEach((k, v) -> System.out.println(k + " => " + v));
        System.out.println();

        spec2.getDelimiters().forEach(delimiter -> System.out.println("delimiter => " + delimiter));
        Assertions.assertEquals("", spec2.getDelimiters().get(0));
        Assertions.assertEquals(",ELE_P,", spec2.getDelimiters().get(1));
    }

    @Test
    public void testInitWithSuffix() {
        RowKeySpec spec2 = RowKeySpec.builder()
                .template("{text:addrIP},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}mysuffix")
                .build();
        spec2.ensureInit();

        spec2.getVariables().forEach((k, v) -> System.out.println(k + " => " + v));
        System.out.println();

        spec2.getDelimiters().forEach(delimiter -> System.out.println("delimiter => " + delimiter));
        Assertions.assertEquals("", spec2.getDelimiters().get(0));
        Assertions.assertEquals("mysuffix", spec2.getDelimiters().get(spec2.getDelimiters().size() - 1));
    }

    @Test
    public void testToRowKey() {
        RowKeySpec spec = RowKeySpec.builder()
                .template("{text:addrIP:},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}")
                .build();
        Map<String, String> parts = new HashMap<>();
        parts.put("addrIP", "12345678");
        parts.put("templateMark", "134");
        parts.put("addrIPOrder", "1");
        String rowKey = spec.to(parts, DateUtils2.formatDate(new Date(), "yyyyMMddHHmmssSSS"));
        System.out.println(rowKey);

        Assertions.assertTrue(startsWith(rowKey, "12345678,ELE_P,134,01,20"));
    }

    @Test
    public void testToRowKey2() {
        RowKeySpec spec = RowKeySpec.builder()
                .template("{date:yyyyMMddHHmmssSSS},{text:addrIP:},ELE_P,{text:templateMark},{text:addrIPOrder:%02d}")
                .build();
        Map<String, String> parts = new HashMap<>();
        parts.put("addrIP", "12345678");
        parts.put("templateMark", "134");
        parts.put("addrIPOrder", "1");
        String rowKey = spec.to(parts, DateUtils2.formatDate(new Date(), "yyyyMMddHHmmssSSS"));
        System.out.println(rowKey);

        Assertions.assertTrue(endsWith(rowKey, ",12345678,ELE_P,134,01"));
    }

    @Test
    public void testFromRowKey() {
        RowKeySpec spec = RowKeySpec.builder()
                .template("{text:addrIP:},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmssSSS}")
                .build();
        Map<String, String> parts = spec.from("12345678,ELE_P,134,01,20221022184113");
        System.out.println(parts);

        Assertions.assertEquals(parts.get(RowKeySpec.DATE_PATTERN_KEY), "20221022184113");
        Assertions.assertEquals(parts.get("addrIP"), "12345678");
        Assertions.assertEquals(parts.get("templateMark"), "134");
        Assertions.assertEquals(parts.get("addrIPOrder"), "01");
    }

    @Test
    public void testFromRowKey2() {
        RowKeySpec spec = RowKeySpec.builder()
                .template("{date:yyyyMMddHHmmssSSS},{text:addrIP:},ELE_P,{text:templateMark},{text:addrIPOrder:%02d}")
                .build();
        Map<String, String> parts = spec.from("20221022184113,12345678,ELE_P,134,01");
        System.out.println(parts);

        Assertions.assertEquals(parts.get(RowKeySpec.DATE_PATTERN_KEY), "20221022184113");
        Assertions.assertEquals(parts.get("addrIP"), "12345678");
        Assertions.assertEquals(parts.get("templateMark"), "134");
        Assertions.assertEquals(parts.get("addrIPOrder"), "01");
    }

}
