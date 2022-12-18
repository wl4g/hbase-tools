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
import java.util.Date;

import org.springframework.beans.factory.InitializingBean;

import com.wl4g.infra.common.io.FileIOUtils;
import com.wl4g.infra.common.lang.SystemUtils2;
import com.wl4g.tools.hbase.phoenix.util.RowKeySpec;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link ToolsProperties}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ToolsProperties implements InitializingBean {

    private File workspaceDir = new File(USER_HOME + "/.phoenix-tools/");
    private int writeSqlLogFileFlushOnBatch = 1024;
    private int writeSqlLogFlushOnMillis = 2000;
    private String tableNamespace = "safeclound";
    private String tableName = "tb_ammeter";
    private boolean dryRun = true;
    private int threadPools = 2;
    private int maxLimit = 1440 * 10;
    private boolean errorContinue = true;
    private long awaitSeconds = Duration.ofMillis(30).getSeconds();
    private RowKeySpec rowKey = new RowKeySpec();
    private String startDate = formatDate(addDays(new Date(), -1), "yyyyMMddHH");
    private String endDate = formatDate(new Date(), "yyyyMMddHH");
    private transient @Setter(AccessLevel.NONE) File undoSqlDir;
    private transient @Setter(AccessLevel.NONE) File redoSqlDir;
    private RunnerProvider provider;

    private FakerProperties faker = new FakerProperties();
    private CleanerProperties cleaner = new CleanerProperties();
    private ExporterProperties exporter = new ExporterProperties();

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!workspaceDir.exists()) {
            FileIOUtils.forceMkdirParent(workspaceDir);
        }
        this.undoSqlDir = new File(workspaceDir, "undo-" + STARTUP_TIME + "-" + SystemUtils2.LOCAL_PROCESS_ID);
        if (!undoSqlDir.exists()) {
            FileIOUtils.forceMkdir(undoSqlDir);
        }
        this.redoSqlDir = new File(workspaceDir, "redo-" + STARTUP_TIME + "-" + SystemUtils2.LOCAL_PROCESS_ID);
        if (!redoSqlDir.exists()) {
            FileIOUtils.forceMkdir(redoSqlDir);
        }
        // Ensure initialized.
        rowKey.ensureInit();
    }

    public static enum RunnerProvider {
        MONOTONE_INCREASE_FAKER, SIMPLE_FAKER, SIMPLE_CLEANER, SIMPLE_EXPORTER;
    }

    public static final long STARTUP_TIME = currentTimeMillis();

}
