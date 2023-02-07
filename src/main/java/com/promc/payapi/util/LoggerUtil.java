package com.promc.payapi.util;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

/**
 * 日志工具
 */
public class LoggerUtil {

    private static final String PREFIX = "[PayAPI] ";

    @Setter
    @Getter
    private static boolean debug;

    public static void debug(String message) {
        if (debug) {
            Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + formatString(message));
        }
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + formatString(message));
    }

    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GOLD + formatString(message));
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + formatString(message));
    }

    public static String formatString(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
