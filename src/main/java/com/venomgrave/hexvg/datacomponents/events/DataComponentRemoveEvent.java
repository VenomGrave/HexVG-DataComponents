package com.venomgrave.hexvg.datacomponents.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataComponentRemoveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled = false;
    private final Player player;
    private final ItemStack item;
    private final String componentName;
    private final Object removedValue;

    public DataComponentRemoveEvent(@Nullable Player player, @NotNull ItemStack item,
                                    @NotNull String componentName, @Nullable Object removedValue) {
        this.player = player;
        this.item = item;
        this.componentName = componentName;
        this.removedValue = removedValue;
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
    public Object getRemovedValue() {
        return removedValue;
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
