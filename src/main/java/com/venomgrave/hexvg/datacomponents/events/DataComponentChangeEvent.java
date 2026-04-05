package com.venomgrave.hexvg.datacomponents.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataComponentChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled = false;
    private final Player player;
    private final ItemStack item;
    private final String componentName;
    private final Object oldValue;
    private Object newValue;

    public DataComponentChangeEvent(@Nullable Player player, @NotNull ItemStack item,
                                    @NotNull String componentName,
                                    @Nullable Object oldValue, @Nullable Object newValue) {
        this.player = player;
        this.item = item;
        this.componentName = componentName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getItem() {
        return item;
    }

    @NotNull
    public String getComponentName() {
        return componentName;
    }

    @Nullable
    public Object getOldValue() {
        return oldValue;
    }

    @Nullable
    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(@Nullable Object newValue) {
        this.newValue = newValue;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
