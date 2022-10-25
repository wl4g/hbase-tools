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

import static com.wl4g.infra.common.lang.DateUtils2.formatDate;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.SystemUtils.USER_HOME;
import static org.apache.commons.lang3.time.DateUtils.addDays;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Min;

import org.springframework.beans.factory.InitializingBean;

import com.wl4g.infra.common.io.FileIOUtils;
import com.wl4g.tools.hbase.phoenix.fake.AbstractColumnFaker.FakeProvider;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link PhoenixFakeProperties}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PhoenixFakeProperties implements InitializingBean {

    private File workspaceDir = new File(USER_HOME + "/.phoenix-fake-tool/");

    private int undoSQLStageFlushOnBatch = 1024;

    private int undoSQLStageFlushOnSeconds = 2;

    private String tableNamespace = "safeclound";

    private String tableName = "tb_ammeter";

    private boolean dryRun = true;

    private int threadPools = 1;

    private int maxLimit = 1440 * 10;

    private boolean errorContinue = false;

    private long awaitSeconds = Duration.ofMillis(30).getSeconds();

    private RowKeySpec rowKey = new RowKeySpec();

    private String startDate = formatDate(addDays(new Date(), -1), "yyyyMMddHH");

    private String endDate = formatDate(new Date(), "yyyyMMddHH");

    /**
     * 时间刻度: 如用于采样时获取过去一段"时间量"的历史数据; </br>
     */
    private String sampleLastDatePattern = "dd";

    /**
     * 时间量: 对应 {@link #sampleLastDatePattern}
     */
    private @Min(1) int sampleLastDateAmount = 1;

    // 注: 当使用 Cumulative Fake(即递增) 模拟数据时, 最小和最大随机百分比应该 >1
    // 反之, 如果生成模拟数据无需递增, 则最小随机百分比可以 <1
    private double valueMinRandomPercent = 1.0124;

    private double valueMaxRandomPercent = 1.0987;

    private List<String> columnNames = new ArrayList<String>() {
        private static final long serialVersionUID = 1L;
        {
            // e.g1: 有功功率, 无功功率
            // e.g2: 有功电度, 无功电度
            add("activePower");
            add("reactivePower");
        }
    };

    private SimpleColumnFakerConfig simple = new SimpleColumnFakerConfig();

    private MonotoneIncreaseColumnFakerConfig monotoneIncrease = new MonotoneIncreaseColumnFakerConfig();

    private FakeProvider provider = FakeProvider.MONOTONE_INCREASE;

    private transient @Setter(AccessLevel.NONE) File undoSqlDir;

    @Override
    public void afterPropertiesSet() throws Exception {
        FileIOUtils.forceMkdirParent(workspaceDir);
        this.undoSqlDir = new File(workspaceDir, "undo-" + currentTimeMillis());
        FileIOUtils.forceMkdir(undoSqlDir);

        // Ensure initialized.
        rowKey.ensureInit();
    }

    @Getter
    @Setter
    @ToString
    public static class SimpleColumnFakerConfig {
    }

    @Getter
    @Setter
    @ToString
    public static class MonotoneIncreaseColumnFakerConfig {

        /**
         * 时间量: 基于过去一段时长(3d)的平均值, 使用那种"时间刻度"参考:
         * {@link PhoenixFakeProperties#sampleLastDatePattern}
         */
        private @Min(1) int sampleBeforeAverageDateAmount = 3;

        // 更简单实现: lastMaxFakeValue * fakeAmount, 无需重试生成.
        // /**
        // * 随机生成满足要求的 fakeValue 值时的最大尝试次数
        // */
        // private int maxAttempts = 10;
        //
        // /**
        // * 最大努力尝试依然无法满足, 则使用: (value + fallbackFakeAmountValue)
        // */
        // private double fallbackFakeAmountValue = 31 * Math.E * Math.PI;
    }

}
