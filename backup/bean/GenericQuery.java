package com.wl4g.tools.hbase.phoenix.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * {@link GenericQuery}
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
public class GenericQuery extends BaseBean {

    // 开始时间
    private String beginTime;

    // 结束时间
    private String endTime;

    private String tableName;
    private String columns; // ，分隔
    private String column;

    private String sortFlag;// 排序标识 1 升序 2降序

    private String isCommon;// 是否查询通用表 1 是

}
