package com.promc.payapi.event;

import com.promc.payapi.PayAPI;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.event.eventinf.OrderEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PayCloseEvent extends OrderEvent {

    private static final HandlerList handlerList = new HandlerList();

    public PayCloseEvent(PayAPI plugin, Order order) {
        super(plugin, order);
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlerList;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
