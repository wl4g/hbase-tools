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
package com.wl4g.tools.hbase.phoenix.config;

import static org.apache.commons.lang3.SystemUtils.USER_HOME;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;

import com.wl4g.infra.common.io.FileIOUtils;
import com.wl4g.infra.common.lang.DateUtils2;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link PhoenixRepairProperties}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PhoenixRepairProperties implements InitializingBean {

    private File metaCsvFile = new File(USER_HOME + "/.phoenix-fixtool/meta.csv");

    private String tableNamespace = "safeclound";

    private String tableName = "tb_ammeter";

    private String startDate = DateUtils2.formatDate(DateUtils2.addDays(new Date(), -2), "yyyyMMddHHmm");

    private String endDate = DateUtils2.formatDate(DateUtils2.addDays(new Date(), -1), "yyyyMMddHHmm");

    private int maxLimit = 1440 * 10;

    private RowKeySpec rowKey = new RowKeySpec();

    private OffsetConfig offset = new OffsetConfig();

    private int threadPools = 1;

    private long awaitSeconds = Duration.ofMillis(30).getSeconds();

    private boolean errorContinue = false;

    private boolean dryRun = true;

    private CumulativeColumnConfig cumulative = new CumulativeColumnConfig();

    @Override
    public void afterPropertiesSet() throws Exception {
        FileIOUtils.forceMkdirParent(metaCsvFile);
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class OffsetConfig {
        private String rowKeyOffsetDatePattern = "dd";
        private int rowKeyOffsetDateAmount = 1;
        private double valueOffsetWithRandomMinPercentage = 0.8976;
        private double valueOffsetWithRandomMaxPercentage = 1.1024;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public static class CumulativeColumnConfig {
        private List<String> columnNames = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;
            {
                add("activePower");
                add("reactivePower");
            }
        };
    }

}
