package com.wl4g.tools.hbase.phoenix.bean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * {@link AmmeterBean}
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
public class AmmeterBean extends BaseBean {
    // 有功电度(单位千瓦时)
    private Double activePower;

    // 无功电度(单位千乏时)
    private Double reactivePower;
}
