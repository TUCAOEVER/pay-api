package com.promc.payapi.event;

import com.promc.payapi.PayAPI;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.event.eventinf.OrderEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * 二维码生成事件
 * 可以通过这个事件美化二维码(异步)
 */
public class QRCodeGenerateEvent extends OrderEvent {

    private static final HandlerList handlerList = new HandlerList();

    private final String qrcodeUrl;

    private BufferedImage qrcodeImage;

    public QRCodeGenerateEvent(PayAPI plugin, Order order, String qrcodeUrl, BufferedImage qrcodeImage) {
        super(plugin, order, true);
        this.qrcodeUrl = qrcodeUrl;
        this.qrcodeImage = qrcodeImage;
    }

    /**
     * 获取二维码的URL
     *
     * @return 二维码URL
     */
    public String getQRCodeUrl() {
        return qrcodeUrl;
    }

    /**
     * 获取二维码图像
     *
     * @return 二维码图像
     */
    public BufferedImage getQRCodeImage() {
        return qrcodeImage;
    }

    /**
     * 设置二维码图像
     *
     * @param qrcodeImage 二维码图像
     */
    public void setQRCodeImage(BufferedImage qrcodeImage) {
        this.qrcodeImage = qrcodeImage;
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