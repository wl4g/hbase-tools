package com.wl4g.tools.hbase.phoenix.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil implements DateFormatImpl {

    /**
     * 把时间字符串转换成毫秒数
     * 
     * @param dateStr
     * @return
     * @throws Exception
     */
    public static String getMilliseconds(String dateStr) throws Exception {
        long millionSeconds = dfyyyyMMddHHMMSS.get().parse(dateStr).getTime();// 毫秒
        return millionSeconds + "";
    }

    /**
     * 把毫秒数转换成时间字符串
     * 
     * @param millionSeconds
     * @return
     */
    public static String getDateStr(String millionSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(millionSeconds));
        return dfyyyyMMddHHMMSS.get().format(calendar.getTime());
    }

    /**
     * 获取当前时间(年月日)
     * 
     * @return
     */
    public static String getCurrentDateStr(Date date) {
        return yyyyMmDd.get().format(date);
    }

    public static String getDateStr(Date date) {
        return dfyyyyMMddHHMMSS.get().format(date);
    }

    public static String getDateFormatStr(Date d, String formart) {
        return getDateParser(formart).format(d);
    }

    public static String getDateFormatStr(String dataStr, String nowFormart, String decFormart) throws Exception {
        Date date = getDateParser(nowFormart).parse(dataStr);
        return getDateParser(decFormart).format(date);
    }

    private static SimpleDateFormat getDateParser(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * 获取前一天
     * 
     * @param date
     * @return
     */
    public static String getLastDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        return yyyyMmDd.get().format(c.getTime());
    }

    /**
     * 获取前n天
     * 
     * @param date
     * @return
     */
    public static String getBeforeDay(Date date, int n) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -n);
        return yyyyMmDd.get().format(c.getTime());
    }

    /**
     * 获取当前时间(年月)
     * 
     * @return
     */
    public static String getMonthDateStr(Date date) {
        return yyyyMm.get().format(date);
    }

    /**
     * 获取当前时间上一个月(年月)
     * 
     * @return
     */
    public static String getLastMonthDateStr(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MONTH, -1);
        return yyyyMm.get().format(cal.getTime());
    }

    public static int getBetweenDates(String date1, String date2) throws Exception {

        int result = 0;

        Calendar calst = Calendar.getInstance();
        ;
        Calendar caled = Calendar.getInstance();

        calst.setTime(yyyyMmDd.get().parse(date1));
        caled.setTime(yyyyMmDd.get().parse(date2));

        // 设置时间为0时
        calst.set(Calendar.HOUR_OF_DAY, 0);
        calst.set(Calendar.MINUTE, 0);
        calst.set(Calendar.SECOND, 0);
        caled.set(Calendar.HOUR_OF_DAY, 0);
        caled.set(Calendar.MINUTE, 0);
        caled.set(Calendar.SECOND, 0);
        // 得到两个日期相差的天数
        result = ((int) (caled.getTime().getTime() / 1000) - (int) (calst.getTime().getTime() / 1000)) / 3600 / 24;

        return result;
    }

    public static int getBetweenSeconds(String date1, String date2) throws Exception {

        int result = 0;

        Calendar calst = Calendar.getInstance();
        ;
        Calendar caled = Calendar.getInstance();

        calst.setTime(dfyyyyMMddHHMMSS.get().parse(date1));
        caled.setTime(dfyyyyMMddHHMMSS.get().parse(date2));

        // 得到两个日期相差的天数
        result = ((int) (caled.getTime().getTime() / 1000) - (int) (calst.getTime().getTime() / 1000));

        return result;
    }

    public static Date getYyyyMmDdDate(String dateStr) throws Exception {
        return yyyyMmDd.get().parse(dateStr);
    }

    public static Date getOldMinuteDate(Date date, Integer n) {
        long c = date.getTime() - 60000 * n.intValue();
        return new Date(c);
    }

    public static long parse(String pattern, String dateSource) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.CHINA);
        try {
            return sdf.parse(dateSource).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
