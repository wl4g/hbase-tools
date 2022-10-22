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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.wl4g.tools.hbase.phoenix.handler.CumulativeColumnFakeHandler;

/**
 * {@link PhoenixFakeAutoConfiguration}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v3.0.0
 */
@Configuration
public class PhoenixFakeAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "fake")
    public PhoenixFakeProperties phoenixFakeProperties() {
        return new PhoenixFakeProperties();
    }

    @Bean
    public CumulativeColumnFakeHandler cumulativeColumnFakeHandler() {
        return new CumulativeColumnFakeHandler();
    }

}
