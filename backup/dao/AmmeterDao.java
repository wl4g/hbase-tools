package com.wl4g.tools.hbase.phoenix.dao;

import java.util.List;
import java.util.Map;

import com.wl4g.tools.hbase.phoenix.bean.AmmeterBean;
import com.wl4g.tools.hbase.phoenix.bean.GenericQuery;

public interface AmmeterDao {

    void saveUpdate(AmmeterBean ammeterBean) throws Exception;

    Map<String, String> getActivePowerDataByCondition(GenericQuery param) throws Exception;

    List<Map<String, Object>> getReactivePowerDataByCondition(GenericQuery param);

    List<Map<String, Object>> getPowerFactorDayDataByCondition(GenericQuery param);

    List<Map<String, Object>> getGroupPowerCountDataByCondition(
            GenericQuery param,
            String rowkey,
            int substrIndex,
            int substrCount);

    List<Map<String, Object>> getPowerByCondition(GenericQuery param);

    Map<String, Double> queryAmmeterRange(String id, String start, String end);

    Map<String, Object> getAmmeter(GenericQuery param) throws Exception;

    Map<String, Object> getTimeQuantumAmmeters(
            String startTime,
            String endTime,
            String addr,
            String order,
            String meterType,
            String type) throws Exception;

}
