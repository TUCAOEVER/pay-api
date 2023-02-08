package com.promc.payapi.api;

import com.google.gson.Gson;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.api.payway.Payway;
import com.promc.payapi.api.storage.Storage;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PayInterface {

    Gson GSON = new Gson();

    /**
     * 获取异步通知地址
     *
     * @return 异步通知地址
     */
    @NotNull
    String getNotifyUrl();

    /**
     * 获取支付方式
     *
     * @param identifier 标识符{@link Payway#getIdentifier()}
     * @return 支付方式
     */
    @NotNull
    Optional<Payway> getPayway(@NotNull String identifier);

    /**
     * 获取所有支付方式
     *
     * @return 支付方式
     */
    @NotNull
    List<Payway> getAllPayway();

    /**
     * 判断玩家是否正在支付
     *
     * @param uuid UUID
     * @return 是否正在支付
     */
    boolean isPaying(@NotNull UUID uuid);

    /**
     * 获取所有正在支付的订单
     *
     * @return 正在支付的订单
     */
    @NotNull
    Map<UUID, Order> getPayingOrders();

    /**
     * 创建订单
     *
     * @param buyer   买家
     * @param amount  支付金额
     * @param subject 标题
     * @return 订单号
     */
    @NotNull
    Order createOrder(@NotNull UUID buyer, @NotNull BigDecimal amount, @NotNull String subject);

    /**
     * 发起支付
     * 一个订单只能发起一次支付 避免造成重复支付
     *
     * @param order  订单
     * @param payway 支付方式
     * @return 订单号
     */
    void initiatePay(@NotNull Order order, @NotNull Payway payway);

    /**
     * 标记付款(尽量不要用吧)
     *
     * @param orderInfo 订单信息
     */
    void markPay(Map<String, String> orderInfo);

    /**
     * 获取存储
     *
     * @return 存储
     */
    @NotNull
    Storage getStorage();
}
