package com.promc.payapi.event.eventinf;

import com.promc.payapi.PayAPI;
import com.promc.payapi.api.order.Order;
import org.jetbrains.annotations.NotNull;

/**
 * 插件订单相关事件的父类
 */
public abstract class OrderEvent extends PayEvent {

    private final Order order;

    public OrderEvent(PayAPI plugin, Order order) {
        this(plugin, order, false);
    }

    public OrderEvent(PayAPI plugin, Order order, boolean isAsync) {
        super(plugin, isAsync);
        this.order = order;
    }

    /**
     * 获取订单号
     *
     * @return 订单号
     */
    @NotNull
    public Order getOrder() {
        return order;
    }
}
