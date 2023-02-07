package com.promc.payapi.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;
import com.promc.payapi.PayAPI;
import com.promc.payapi.api.order.Order;
import com.promc.payapi.event.PayCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.*;

/**
 * 支付监听器
 */
public class PayListener extends PacketAdapter implements Listener {

    public PayListener(PayAPI plugin) {
        super(plugin, PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Client.BLOCK_DIG);
    }

    /**
     * 监听服务器发出的 WindowItems 数据包
     * 如果 WindowItems 包被重新发送 则退出支付
     */
    @Override
    public void onPacketSending(PacketEvent event) {
        Map<UUID, Order> paying = ((PayAPI) plugin).getPayingOrders();
        Player player = event.getPlayer();
        Order order = paying.remove(player.getUniqueId());
        if (order != null) {
            // 拉回主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                PayCloseEvent payCloseEvent = new PayCloseEvent((PayAPI) plugin, order);
                Bukkit.getPluginManager().callEvent(payCloseEvent);
            });
        }
    }

    /**
     * 监听服务器接收的 PlayerAction 包
     * 玩家按下Q键后更新库存 退出支付
     */
    @Override
    public void onPacketReceiving(PacketEvent event) {
        PlayerDigType digType = event.getPacket().getPlayerDigTypes().read(0);
        if (digType == PlayerDigType.DROP_ITEM || digType == PlayerDigType.DROP_ALL_ITEMS) {
            // 拉回主线程
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = event.getPlayer();
                if (player.isOnline()) {
                    player.updateInventory();
                }
            });
        }
    }

    /**
     * 点击库存退出支付
     *
     * @param event 库存点击事件
     */
    @EventHandler
    public void onClickInv(InventoryClickEvent event) {
        HumanEntity clicked = event.getWhoClicked();
        if (clicked instanceof Player) {
            if (PayAPI.getAPI().isPaying(clicked.getUniqueId())) {
                ((Player) clicked).updateInventory();
                event.setCancelled(true);
            }
        }
    }

    /**
     * 拾取物品退出支付
     *
     * @param event 拾取物品事件
     */
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) {
            if (PayAPI.getAPI().isPaying(entity.getUniqueId())) {
                ((Player) entity).updateInventory();
                event.setCancelled(true);
            }
        }
    }

    /**
     * 丢弃物品时退出支付
     *
     * @param event 丢去物品事件
     */
    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (PayAPI.getAPI().isPaying(event.getPlayer().getUniqueId())) {
            event.getPlayer().updateInventory();
            event.setCancelled(true);
        }
    }

    /**
     * 切换副手时
     *
     * @param event 切换副手事件
     */
    @EventHandler
    public void onToggleHand(PlayerChangedMainHandEvent event) {
        if (PayAPI.getAPI().isPaying(event.getPlayer().getUniqueId())) {
            event.getPlayer().updateInventory();
        }
    }

}
