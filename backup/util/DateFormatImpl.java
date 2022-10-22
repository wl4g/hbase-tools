/*
 * Copyright 2017 ~ 2045 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"));
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

import java.text.SimpleDateFormat;
import static java.lang.ThreadLocal.withInitial;

/**
 * {@link DateFormatImpl}
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v2.0 2020-08-28
 * @author zhaox
 * @version v1.0 2016-08-28
 * @since
 */
public abstract interface DateFormatImpl {

    public static final String FORMAT_DateTime = "yyyy-MM-dd HH:mm:ss";

    public static final String FORMAT_DateTime_12 = "yyyy-MM-dd hh:mm:ss";

    public static final String FORMAT_Date = "yyyy-MM-dd";

    public static final String FORMAT_Time = "HH:mm:ss";

    public static final String FORMAT_Time_12 = "hh:mm:ss";

    public static final String DF_YYYY = "yyyy";

    public static final String DF_MM = "MM";

    public static final String DF_DD = "dd";
    public static final String DF_MMMYYYY = "MMM yyyy";

    public static final String DF_DDMMMYYYY = "dd MMM yyyy";

    public static final String DF_MMDDYYYY = "MM/dd/yyyy";

    public static final String DF_YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";

    public static final String DF_YYYYMMDDHHMMSSS = "yyyy-MM-dd HH:mm:ss:SSS";
    public static final String DF_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DF_YYYYMMDDHH = "yyyyMMddHH";
    public static final String DF_HHMM = "HH:mm";

    public static final String DF_HHMMSS = "HHmmss";

    public static final String DF_YYYYMMDD = "yyyyMMdd";

    public static final String DF_YYYYMM = "yyyyMM";

    public static final String DF_YYYY_MM = "yyyy-MM";

    public static final String DF_YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static final String DF_YYYYMMDDHHMM = "yyyyMMddHHmm";

    public static final String DF_YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";

    public static final ThreadLocal<SimpleDateFormat> dfYyyy = withInitial(() -> new SimpleDateFormat("yyyy"));

    public static final ThreadLocal<SimpleDateFormat> dfMM = withInitial(() -> new SimpleDateFormat("MM"));

    public static final ThreadLocal<SimpleDateFormat> dfDd = withInitial(() -> new SimpleDateFormat("dd"));

    public static final ThreadLocal<SimpleDateFormat> dfMmmYyyy = withInitial(() -> new SimpleDateFormat("MMM yyyy"));

    public static final ThreadLocal<SimpleDateFormat> dfDdMMMYYYY = withInitial(() -> new SimpleDateFormat("dd MMM yyyy"));

    public static final ThreadLocal<SimpleDateFormat> dfMmDdYYYY = withInitial(() -> new SimpleDateFormat("MM/dd/yyyy"));

    public static final ThreadLocal<SimpleDateFormat> dfyyyyMMddHHMMSS = withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static final ThreadLocal<SimpleDateFormat> dfyyyyMMddHHMMSSS = withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMmDd = withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMm = withInitial(() -> new SimpleDateFormat("yyyy-MM"));

    public static final ThreadLocal<SimpleDateFormat> hhmm = withInitial(() -> new SimpleDateFormat("HH:mm"));

    public static final ThreadLocal<SimpleDateFormat> yyyymmdd = withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    public static final ThreadLocal<SimpleDateFormat> yyyymm = withInitial(() -> new SimpleDateFormat("yyyyMM"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMMddHH = withInitial(() -> new SimpleDateFormat("yyyyMMddHH"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMMdd_HH = withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMMdd_HH_mm = withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd HH:mm"));

    public static final ThreadLocal<SimpleDateFormat> yyyyMMddHHmmss = withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    public static final ThreadLocal<SimpleDateFormat> dfyyyyMMddHHMMSSSSS = withInitial(
            () -> new SimpleDateFormat("yyyyMMddHHmmssSSS"));

    public static final ThreadLocal<SimpleDateFormat> dfDF_HHMMSS = withInitial(() -> new SimpleDateFormat("HHmmss"));

    public static final ThreadLocal<SimpleDateFormat> dfDf_24HHMMSS = withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    public static final String twentyFourHourRegExp = "^(([0-1][0-9])|(2[0-3])):([0-5][0-9])$";

}
