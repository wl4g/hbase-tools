package com.wl4g.tools.hbase.phoenix.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * {@link BaseBean}
 * 
 * @author James Wong
 * @version 2022-10-22
 * @since v1.0.0
 */
@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
public class BaseBean {

    /**
     * DTU地址
     */
    private String addr;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 设备标识
     */
    private String meterType;

    /**
     * 传感器地址
     */
    private String order;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 创建时间yyyyMMddHHmmssSSS
     */
    private String createDate;

    /**
     * 客户id
     */
    private String cid;

    /**
     * 客户分中心id
     */
    private String bid;

}
