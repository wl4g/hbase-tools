package com.wl4g.tools.hbase.phoenix.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.wl4g.tools.hbase.phoenix.bean.AmmeterBean;
import com.wl4g.tools.hbase.phoenix.bean.GenericQuery;
import com.wl4g.tools.hbase.phoenix.config.PhoenixHbaseToolsProperties;
import com.wl4g.tools.hbase.phoenix.dao.AmmeterDao;
import com.wl4g.tools.hbase.phoenix.util.DateUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link AmmeterDaoImpl}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 */
@Slf4j
@Repository
public class AmmeterDaoImpl implements AmmeterDao {

    private @Autowired JdbcTemplate jdbcTemplate;
    private @Resource PhoenixHbaseToolsProperties config;

    @Override
    @Transactional
    public void saveUpdate(AmmeterBean ammeterBean) throws Exception {
        String sql = config.getSaveOrUpdateAmmeter();
        long currentTimeMillis = ammeterBean.getTimestamp();
        jdbcTemplate.update(sql, ammeterBean.getAddr(), ammeterBean.getDataType(), ammeterBean.getMeterType(),
                ammeterBean.getOrder(), ammeterBean.getActivePower(), ammeterBean.getReactivePower(),
                Long.toString(currentTimeMillis));
    }

    @Override
    public Map<String, Object> getTimeQuantumAmmeters(
            String startTime,
            String endTime,
            String addr,
            String order,
            String meterType,
            String type) throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("addr", addr);
        resultMap.put("order", order);
        resultMap.put("startNum", "0");
        resultMap.put("endNum", "0");
        resultMap.put("amount", "0");
        String rowKey1 = addr + ",ELE_P," + meterType + "," + order + "," + startTime + "000";
        String rowKey2 = addr + ",ELE_P," + meterType + "," + order + "," + endTime + "000";
        String sql = "select min(TO_NUMBER(t.\"activePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        String sql2 = "select max(TO_NUMBER(t.\"activePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        if ("1".equals(type)) {// 查询无功电度
            sql = "select min(TO_NUMBER(t.\"reactivePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
            sql2 = "select max(TO_NUMBER(t.\"reactivePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        }

        String endStr = endTime;
        endStr = endStr.substring(0, 4) + "-" + endStr.substring(4, 6) + "-" + endStr.substring(6, 8);
        Date date = DateUtil.getYyyyMmDdDate(endStr);
        String dayStr = DateUtil.getBeforeDay(date, -1);
        String endStri1 = DateUtil.getDateFormatStr((dayStr + " 00:00:00"), DateUtil.DF_YYYY_MM_DD_HHMMSS,
                DateUtil.DF_YYYYMMDDHHMMSSSSS);
        String rowKey3 = addr + ",ELE_P," + meterType + "," + order + "," + endStri1;
        List<Map<String, Object>> result1 = jdbcTemplate.queryForList(sql, rowKey1, rowKey2);
        List<Map<String, Object>> result2 = jdbcTemplate.queryForList(sql, rowKey2, rowKey3);
        if (result1 != null && result1.size() > 0 && result2 != null && result2.size() > 0) {
            double startNum = 0.0, endNum = 0.0, amount = 0.0;
            Map<String, Object> bm = result1.get(0);
            Object bObj = bm.get("POWER");
            if (bObj != null) {
                startNum = Double.parseDouble(String.valueOf(bObj));
                resultMap.put("startNum", String.valueOf(bObj));
            }

            if (result2.get(0).get("POWER") == null) {
                result2 = jdbcTemplate.queryForList(sql2, rowKey1, rowKey2);
            }
            Map<String, Object> bm2 = result2.get(0);
            Object bObj2 = bm2.get("POWER");
            if (bObj2 != null) {
                endNum = Double.parseDouble(String.valueOf(bObj2));
                resultMap.put("endNum", String.valueOf(bObj2));
            }
            amount = endNum - startNum;
            resultMap.put("amount", String.format("%.2f", amount));
        }
        return resultMap;
    }

    @Override
    public Map<String, String> getActivePowerDataByCondition(GenericQuery param) throws Exception {
        Map<String, String> resultMap = new HashMap<String, String>();
        resultMap.put("POWER", "0");
        String beginDay = param.getBeginTime().substring(0, 8);
        String endStr = param.getEndTime();
        endStr = endStr.substring(0, 4) + "-" + endStr.substring(4, 6) + "-" + endStr.substring(6, 8);
        Date date = DateUtil.getYyyyMmDdDate(endStr);
        String dayStr = DateUtil.getBeforeDay(date, -1);
        String endStri1 = DateUtil.getDateFormatStr((dayStr + " 00:00:00"), DateUtil.DF_YYYY_MM_DD_HHMMSS,
                DateUtil.DF_YYYYMMDDHHMMSSSSS);
        String rowKey1 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getBeginTime();
        String rowKey2 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getEndTime();
        String rowKey3 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + endStri1;
        // log.info("rowKey1:" + rowKey1 + ",rowKey2:" + rowKey2 + ",rowKey3:" +
        // rowKey3);

        String sql = "select min(TO_NUMBER(t.\"activePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        List<Map<String, Object>> result1 = jdbcTemplate.queryForList(sql, rowKey1, rowKey2);
        List<Map<String, Object>> result2 = null;

        // (特殊情况)为解决查询起始时间为今日的电量，会出现result2永远返回都是null的问题
        //
        // List<Map<String, Object>> result3 = null;
        // if (DateFormatUtils.format(new Date(), "yyyyMMdd").equals(beginDay))
        // { // 查询起始时间是今日，需特殊处理

        // }
        // 正常情况（查询的结束日期是小于今日的）
        // else {
        result2 = jdbcTemplate.queryForList(sql, rowKey2, rowKey3);
        // }

        if (result1 != null && result1.size() > 0) {
            double begin = 0.0, end = 0.0, differ = 0.0;
            Map<String, Object> bm = result1.get(0);
            Object bObj = bm.get("POWER");
            if (bObj != null) {
                begin = Double.parseDouble(String.valueOf(bObj));
            }

            // 处理正常情况结果
            if (result2 != null && result2.size() > 0) {
                Map<String, Object> em = result2.get(0);
                Object eObj = em.get("POWER");
                if (eObj != null) {
                    end = Double.parseDouble(String.valueOf(eObj));
                }

            }
            // 处理特殊情况结果
            if (DateFormatUtils.format(new Date(), "yyyyMMdd").equals(beginDay) || end == 0.0) {
                String sql2 = "select max(TO_NUMBER(t.\"activePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
                List<Map<String, Object>> result3 = jdbcTemplate.queryForList(sql2, rowKey1, rowKey2);
                // log.info("查询起始时间为今日sql:" + sql2);
                if (result3 != null && result3.size() > 0) {
                    Map<String, Object> em = result3.get(0);
                    Object eObj = em.get("POWER");
                    if (eObj != null) {
                        end = Double.parseDouble(String.valueOf(eObj));
                    }
                }

            }
            differ = end - begin;

            // log.info("begin:" + begin + ",end:" + end);
            if (differ < 0)
                differ = 0d;
            resultMap.put("POWER", String.format("%.2f", differ));
        }

        return resultMap;
    }

    @Override
    public List<Map<String, Object>> getReactivePowerDataByCondition(GenericQuery param) {
        String rowKey1 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getBeginTime();
        String rowKey2 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getEndTime();
        String sql = "select max(TO_NUMBER(t.\"reactivePower\"))-min(TO_NUMBER(t.\"reactivePower\")) as POWER from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        return jdbcTemplate.queryForList(sql, rowKey1, rowKey2);
    }

    @Override
    public List<Map<String, Object>> getPowerFactorDayDataByCondition(GenericQuery param) {
        String rowKey1 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getBeginTime();
        String rowKey2 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getEndTime();
        String sql = "select reverse(substr(reverse(\"ROW\"),10,8)) \"date\", max(TO_NUMBER(\"reactivePower\"))-min(TO_NUMBER(\"reactivePower\")) REACTICEPOWER,max(TO_NUMBER(\"activePower\"))-min(TO_NUMBER(\"activePower\")) ACTIVEPOWER from \"safeclound\".\"tb_ammeter\" where \"ROW\">=? and \"ROW\"<=? group by substr(reverse(\"ROW\"),10,8) order by reverse(substr(reverse(\"ROW\"),10,8))";
        return jdbcTemplate.queryForList(sql, rowKey1, rowKey2);
    }

    @Override
    public List<Map<String, Object>> getGroupPowerCountDataByCondition(
            GenericQuery param,
            String rowkey,
            int substrIndex,
            int substrCount) {
        String rowKey1 = rowkey + param.getBeginTime();
        String rowKey2 = rowkey + param.getEndTime();
        String sql = "select substr(\"ROW\"," + substrIndex + "," + substrCount
                + ") \"x_name\",max(TO_NUMBER(\"activePower\"))-min(TO_NUMBER(\"activePower\")) x_value from \"safeclound\".\"tb_ammeter\" where \"ROW\">=? and \"ROW\"<=? group by substr(\"ROW\","
                + substrIndex + "," + substrCount + ") order by substr(\"ROW\"," + substrIndex + "," + substrCount + ")";
        return jdbcTemplate.queryForList(sql, rowKey1, rowKey2);
    }

    @Override
    public List<Map<String, Object>> getPowerByCondition(GenericQuery param) {
        String begin = param.getBeginTime();
        String end = param.getEndTime();
        String rowkey = param.getAddr() + ",%,%," + param.getOrder() + ",";

        String sql = "select substr(\"ROW\",0,(length(\"ROW\")-10)) \"type\",sum(to_number(\"activePower\")) \"value\" from \"safeclound\".\"tb_ammeter_analyze\" where substr(\"ROW\",0,(length(\"ROW\")-10)) like ? and substr(\"ROW\",(length(\"ROW\")-9),10)>=? and substr(\"ROW\",(length(\"ROW\")-9),10)<=? group by substr(\"ROW\",0,(length(\"ROW\")-10))";
        return this.jdbcTemplate.queryForList(sql, new Object[] { rowkey, begin, end });
    }

    @Override
    public Map<String, Double> queryAmmeterRange(String id, String start, String end) {
        String sql = config.getSelectAmmeter();
        String execSql = String.format(sql, id + "%", start, end);
        Map<String, Double> map = jdbcTemplate.query(execSql, new ResultSetExtractor<Map<String, Double>>() {
            @Override
            public Map<String, Double> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<String, Double> res = new HashMap<>();
                while (rs.next()) {
                    String yyyyMMddHH = rs.getString("yyyyMMddHH");
                    String valStr = rs.getString("val");
                    if (StringUtils.isEmpty(valStr))
                        valStr = "0.0";
                    Double val = Double.parseDouble(valStr);
                    res.put(yyyyMMddHH, val);
                }
                return res;
            }
        });
        return map;
    }

    @Override
    public Map<String, Object> getAmmeter(GenericQuery param) throws Exception {
        String rowKey1 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getBeginTime();
        String rowKey2 = param.getAddr() + "," + param.getDataType() + "," + param.getMeterType() + "," + param.getOrder() + ","
                + param.getEndTime();
        String sql = "select (max(to_number(t.\"activePower\"))-min(to_number(t.\"activePower\"))) activePower, (max(to_number(t.\"reactivePower\"))-min(to_number(t.\"reactivePower\"))) reactivePower from \"safeclound\".\"tb_ammeter\" as t where t.\"ROW\" >=? and t.\"ROW\" <=?";
        log.info(String.format("sql=> %s, params=> %s, %s", sql, rowKey1, rowKey2));

        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, rowKey1, rowKey2);

        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public static void main(String[] args) {
        double e = Double.parseDouble(String.valueOf("38063.9"));
        double b = Double.parseDouble(String.valueOf("38062.9"));
        System.out.println(b - e);

    }

}
