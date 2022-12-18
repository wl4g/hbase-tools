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

import static com.wl4g.infra.common.collection.CollectionUtils2.safeList;
import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.StringUtils2.eqIgnCase;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.synchronizedList;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.replaceIgnoreCase;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 
 * {@link RowKeySpec}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v3.0.0
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
public class RowKeySpec {

    /**
     * e.g: 11111111,ELE_P,111,08,20170729165254063
     */
    private @Default String template = DEFAULT_FORMAT;
    private @Default String name = "ROW";

    private volatile transient Map<String, PartVariable> variables;
    private volatile transient List<String> delimiters;

    public String to(@NotNull Map<String, String> parts, @NotBlank String rowDate) {
        notNullOf(parts, "rowKeyParts");
        hasTextOf(rowDate, "rowDate");
        ensureInit();

        String rowKeyWithUnresolveDate = template;

        // Resolve rowKey without date.
        for (Entry<String, String> e : safeMap(parts).entrySet()) {
            String columnName = e.getKey();
            String value = e.getValue();
            PartVariable variable = variables.get(columnName);
            if (nonNull(variable) && eqIgnCase(variable.getType(), TEXT_TYPE)) {
                StringBuilder rowKeyWithoutDateFormat = new StringBuilder("{".concat(TEXT_TYPE).concat(":").concat(columnName));
                if (!isBlank(variable.getFormat())) {
                    rowKeyWithoutDateFormat.append(":");
                    rowKeyWithoutDateFormat.append(variable.getFormat());
                }
                if (!isBlank(variable.getFormat())) {
                    value = format(variable.getFormat(), parseInt(value));
                }
                // e.g:
                // {text:addrIP:},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmss}
                rowKeyWithUnresolveDate = replaceIgnoreCase(rowKeyWithUnresolveDate,
                        rowKeyWithoutDateFormat.toString().concat(":}"), value);
                // e.g:
                // {text:addrIP},ELE_P,{text:templateMark},{text:addrIPOrder:%02d},{date:yyyyMMddHHmmss}
                rowKeyWithUnresolveDate = replaceIgnoreCase(rowKeyWithUnresolveDate,
                        rowKeyWithoutDateFormat.toString().concat("}"), value);
            }
        }

        // Resolve row key date parts.
        String rowKeyDatePattern = variables.get(DATE_PATTERN_KEY).getName();
        String rowKeyDateFormat = "{".concat(DATE_TYPE).concat(":").concat(rowKeyDatePattern).concat("}");

        return replaceIgnoreCase(rowKeyWithUnresolveDate, rowKeyDateFormat, rowDate);
    }

    public Map<String, String> from(final @NotBlank String rowKey) {
        hasTextOf(rowKey, "rowKey");
        ensureInit();

        // 模版变量与分隔符错位拼接(注:分隔符先开始,且数量相等)
        Map<String, String> parts = new LinkedHashMap<>();
        List<PartVariable> _variables = safeList(safeMap(variables).values());
        String decrement = rowKey;
        for (int i = 0; i < delimiters.size(); i++) {
            String delimiter = delimiters.get(i); // for skip
            int index = decrement.indexOf(delimiter);
            String part = decrement.substring(0, index);
            if (i > 0) {
                PartVariable pv = _variables.get(i - 1);
                if (pv.getType().equals(DATE_TYPE)) {
                    parts.put(DATE_PATTERN_KEY, part);
                } else {
                    parts.put(pv.getName(), part);
                }
                decrement = decrement.substring(index + delimiter.length(), decrement.length());
            }
        }

        // 剩最后一个模版变量需单独处理.
        PartVariable pv = _variables.get(_variables.size() - 1);
        if (pv.getType().equals(DATE_TYPE)) {
            parts.put(DATE_PATTERN_KEY, decrement);
        } else {
            parts.put(pv.getName(), decrement);
        }

        return parts;
    }

    public RowKeySpec ensureInit() {
        if (nonNull(variables) && nonNull(delimiters)) {
            return this;
        }

        synchronized (this) {
            this.variables = synchronizedMap(new LinkedHashMap<>());
            this.delimiters = synchronizedList(new ArrayList<>());

            int opens = 0, closes = 0;
            char[] chars = trimToEmpty(template).toCharArray();
            String currentDelimiter = "";
            for (int i = 0, open = -1, close = -1; i < chars.length; i++) {
                char c = chars[i];
                if (c == '{') {
                    open = i;
                    ++opens;
                    delimiters.add(currentDelimiter);
                    currentDelimiter = ""; // Reset.
                } else if (c == '}') {
                    close = i;
                    ++closes;
                    String variable = new String(copyOfRange(chars, open + 1, close));
                    String variableType = ""; // variable type(TEXT|DATE)
                    String variableName = ""; // variable name.
                    String variableFormat = ""; // Optional
                    String[] parts = split(variable, ":");
                    if (nonNull(parts) && parts.length >= 2) {
                        variableType = parts[0];
                        variableName = parts[1];
                        if (parts.length >= 3) {
                            variableFormat = parts[2];
                        }
                        if (eqIgnCase(variableType, TEXT_TYPE)) {
                            variables.put(variableName,
                                    PartVariable.builder().type(variableType).name(variableName).format(variableFormat).build());
                        } else if (eqIgnCase(variableType, DATE_TYPE)) {
                            // e.g:{text:addrIP},ELE_P,{text:templateMark},{text:addrIPOrder},{date:yyyyMMddHHmmss}
                            // =>
                            // Map{addrIP=text,templateMark=text,addrIPOrder=text,_DATE_PATTERN=yyyyMMddHHmmss}
                            variables.put(DATE_PATTERN_KEY,
                                    PartVariable.builder().type(variableType).name(variableName).format(variableFormat).build());
                        } else {
                            throw new UnsupportedOperationException(
                                    String.format("Invalid template variable convertType for : %s, supported: %s, %s", variable,
                                            TEXT_TYPE, DATE_TYPE));
                        }
                    }
                } else if ((close > open && i > close) || open == -1) {
                    currentDelimiter += c;
                    if (i >= chars.length - 1) {
                        delimiters.add(currentDelimiter);
                    }
                }
            }
            if (opens != closes) {
                throw new IllegalArgumentException(format(
                        "Invalid row key template of: '%s', '{' count is %s, but '}' count is %s.", template, opens, closes));
            }
        }

        return this;
    }

    @Getter
    @Setter
    @ToString
    @SuperBuilder
    public static class PartVariable {
        private String type;
        private String name;
        private String format;
    }

    public static final String TEXT_TYPE = "text";
    public static final String DATE_TYPE = "date";
    public static final String DATE_PATTERN_KEY = "__DATE_PATTERN";
    public static final String DEFAULT_FORMAT = "{" + TEXT_TYPE + ":addrIP:},ELE_P,{" + TEXT_TYPE + ":templateMark:},{"
            + TEXT_TYPE + ":addrIPOrder:%02d},{" + DATE_TYPE + ":yyyyMMddHHmmssSSS}";

}