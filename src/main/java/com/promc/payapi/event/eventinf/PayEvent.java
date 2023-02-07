package com.promc.payapi.event.eventinf;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 插件的所有事件的父类
 */
public abstract class PayEvent extends Event {

    private final Plugin plugin;

    public PayEvent(Plugin plugin) {
        this(plugin, false);
    }

    public PayEvent(Plugin plugin, boolean isAsync) {
        super(isAsync);
        this.plugin = plugin;
    }

    @NotNull
    public Plugin getPlugin() {
        return plugin;
    }
}
