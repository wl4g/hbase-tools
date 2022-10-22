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

import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.util.Arrays.copyOfRange;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.LinkedHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * {@link VariableParseUtil}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v3.0.0
 */
public class VariableParseUtil {

    public static LinkedHashMap<String, VariableInfo> parseVariables(String format) {
        LinkedHashMap<String, VariableInfo> variables = new LinkedHashMap<>();

        char[] chars = trimToEmpty(format).toCharArray();
        for (int i = 0, open = 0, close = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '{') {
                open = i;
            } else if (c == '}') {
                close = i;
                String variable = new String(copyOfRange(chars, open + 1, close));
                String variableType = ""; // variable type(CSV|DATE)
                String variableName = ""; // variable name.
                String variableFormat = ""; // Optional
                String[] parts = split(variable, ":");
                if (nonNull(parts) && parts.length >= 2) {
                    variableType = parts[0];
                    variableName = parts[1];
                    if (parts.length >= 3) {
                        variableFormat = parts[2];
                    }
                    if (eqIgnCase(variableType, CSV_TYPE)) {
                        variables.put(variableName,
                                VariableInfo.builder().type(variableType).name(variableName).format(variableFormat).build());
                    } else if (eqIgnCase(variableType, DATE_TYPE)) {
                        // e.g:{csv:addrIP},ELE_P,{csv:templateMark},{csv:addrIPOrder},{date:yyyyMMddHHmmss}
                        // =>
                        // Map{templateMark=csv,addrIPOrder=csv,_DATE_PATTERN=yyyyMMddHHmmss}
                        variables.put(DATE_PATTERN_KEY,
                                VariableInfo.builder().type(variableType).name(variableName).format(variableFormat).build());
                    } else {
                        throw new UnsupportedOperationException(
                                String.format("Invalid format variable convertType for : %s, supported: %s, %s", variable,
                                        CSV_TYPE, DATE_TYPE));
                    }
                }
            }
        }
        return variables;
    }

    @Getter
    @Setter
    @ToString
    @SuperBuilder
    public static class VariableInfo {
        private String type;
        private String name;
        private String format;
    }

    public static final String CSV_TYPE = "csv";
    public static final String DATE_TYPE = "date";
    public static final String DATE_PATTERN_KEY = "__DATE_PATTERN";

}
