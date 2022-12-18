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

import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.addHours;
import static org.apache.commons.lang3.time.DateUtils.addMinutes;
import static org.apache.commons.lang3.time.DateUtils.addMonths;
import static org.apache.commons.lang3.time.DateUtils.addSeconds;
import static org.apache.commons.lang3.time.DateUtils.addYears;

import java.text.ParseException;
import java.util.Date;

/**
 * {@link DateTool}
 * 
 * @author James Wong
 * @version 2022-12-08
 * @since v1.0.0
 */
public abstract class DateTool {

    public static Date getOffsetDate(Date date, String datePattern, int dateAmount) throws ParseException {
        switch (datePattern) {
        case "yy":
        case "y":
        case "YY":
        case "Y":
            date = addYears(date, dateAmount);
            break;
        case "MM":
        case "M":
            date = addMonths(date, dateAmount);
            break;
        case "dd":
        case "d":
        case "DD":
        case "D":
            date = addDays(date, dateAmount);
            break;
        case "HH":
        case "H":
        case "hh":
        case "h":
            date = addHours(date, dateAmount);
            break;
        case "mm":
        case "m":
            date = addMinutes(date, dateAmount);
            break;
        case "ss":
        case "s":
            date = addSeconds(date, dateAmount);
            break;
        }

        return date;
    }

}
