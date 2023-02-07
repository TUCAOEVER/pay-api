package com.promc.payapi.api.payway;

import com.promc.payapi.api.order.Order;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 支付方式
 */
public interface Payway {

    /**
     * 支付方式的标识符<br/>
     * 很重要
     *
     * @return 标识符
     */
    @NotNull
    String getIdentifier();

    /**
     * 发起Native支付
     *
     * @param order 订单号
     * @return 支付二维码地址
     */
    @Nullable
    String nativePay(@NotNull Order order);

    /**
     * 支付方式的异步通知
     *
     * @param ctx     上下文
     * @param request HTTP请求
     */
    void notify(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request);
}
