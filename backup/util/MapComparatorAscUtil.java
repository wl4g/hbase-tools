package com.wl4g.tools.hbase.phoenix.util;

import java.util.Comparator;
import java.util.Map;

public class MapComparatorAscUtil implements Comparator<Map<String, Object>> {
    String key;

    public MapComparatorAscUtil(String key) {
        this.key = key;
    }

    @Override
    public int compare(Map<String, Object> m1, Map<String, Object> m2) {

        try {
            Long v1 = Long.valueOf(DateUtil.getDateFormatStr(m1.get(key).toString(), DateUtil.DF_YYYY_MM_DD_HHMMSS,
                    DateUtil.DF_YYYYMMDDHHMMSSSSS));
            Long v2 = Long.valueOf(DateUtil.getDateFormatStr(m2.get(key).toString(), DateUtil.DF_YYYY_MM_DD_HHMMSS,
                    DateUtil.DF_YYYYMMDDHHMMSSSSS));
            if (v1 != null) {
                return v1.compareTo(v2);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

}
