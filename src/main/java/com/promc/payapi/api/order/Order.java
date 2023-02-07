package com.promc.payapi.api.order;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * 订单号
 */
@Data
@EqualsAndHashCode(of = "id")
public class Order {

    /**
     * 订单号
     */
    private long id;

    /**
     * 买家
     */
    private UUID buyer;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单金额
     */
    private BigDecimal totalFee;

    /**
     * 订单状态
     */
    private int status;

    /**
     * 创建事件
     */
    private Timestamp createTime;

    // 下方只有付款后才会存在

    /**
     * 支付事件
     */
    private Timestamp payTime;

    /**
     * 支付信息
     */
    private String payInfo;
}