package com.promc.payapi.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class MapUtil {

    /**
     * 发送地图数据包
     *
     * @param player        玩家
     * @param bufferedImage 图像
     */
    public static void sendMapViewPacket(Player player, BufferedImage bufferedImage) {
        try {
            Class<?> worldMapClass = Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap$b");
            sendMapViewPacket17(player, bufferedImage, worldMapClass);
        } catch (ClassNotFoundException e) {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.MAP);
            byte[] bytes = MapColor.getByte(bufferedImage);
            packet.getIntegers().write(0, 0);
            packet.getIntegers().write(1, 0);
            packet.getIntegers().write(2, 0);
            packet.getIntegers().write(3, 128);
            packet.getIntegers().write(4, 128);
            packet.getBytes().write(0, (byte) 0);
            packet.getByteArrays().write(0, bytes);
            packet.getBooleans().write(0, true);
            // packet.getBooleans().write(1, true);
            try {
                protocolManager.sendServerPacket(player, packet);
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static void sendMapViewPacket17(Player player, BufferedImage bufferedImage, Class<?> worldMapClass) {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.MAP);
            Constructor<?> constructor = worldMapClass.getConstructor(int.class, int.class, int.class, int.class, byte[].class);
            Object o = constructor.newInstance(0, 0, 128, 128, MapColor.getByte(bufferedImage));
            packet.getModifier().write(4, o);
            try {
                protocolManager.sendServerPacket(player, packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 构建地图地图
     *
     * @return 物品
     */
    public static ItemStack buildMapItem() {
        ItemStack itemStack = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
        mapMeta.setMapId(0);
        itemStack.setItemMeta(mapMeta);
        return itemStack;
    }

    /**
     * 将地图物品给到玩家手里
     *
     * @param player 玩家
     * @param map    地图物品
     */
    public static void sendMapItemPacket(Player player, ItemStack map) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.WINDOW_ITEMS);
        ArrayList<ItemStack> items = new ArrayList<>(46);
        int slot = 36 + player.getInventory().getHeldItemSlot();
        for (int i = 0; i < 46; i++) {
            items.add(new ItemStack(Material.AIR));
        }
        items.set(slot, map);
        packet.getIntegers().write(0, 0);
        packet.getItemListModifier().write(0, items);
        try {
            protocolManager.sendServerPacket(player, packet, false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
