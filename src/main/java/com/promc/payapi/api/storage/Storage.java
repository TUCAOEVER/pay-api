package com.promc.payapi.api.storage;

import com.promc.payapi.api.order.Order;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 存储
 */
public interface Storage {

    String TABLE_NAME = "pay_order";

    /**
     * 创建表
     */
    void createTable();

    /**
     * 插入订单号
     *
     * @param order 订单号
     */
    void insertOrder(@NotNull Order order);

    /**
     * 获取订单
     *
     * @param orderId 订单号
     * @return 订单
     */
    @Nullable
    Order selectOrderById(long orderId);

    /**
     * 标记支付
     *
     * @param orderId 订单号
     * @param info    订单信息
     */
    boolean markPay(long orderId, @NotNull String info);
}
