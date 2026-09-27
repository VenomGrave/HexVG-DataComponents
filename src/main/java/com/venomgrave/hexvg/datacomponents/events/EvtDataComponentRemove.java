package com.venomgrave.hexvg.datacomponents.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import com.venomgrave.hexvg.datacomponents.utils.EventValueRegistrar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EvtDataComponentRemove extends SimpleEvent {

    static {
        Skript.registerEvent(
                "Data Component Remove",
                EvtDataComponentRemove.class,
                DataComponentRemoveEvent.class,
                "data component remove"
        );

        EventValueRegistrar.register(DataComponentRemoveEvent.class, Player.class,
                DataComponentRemoveEvent::getPlayer, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentRemoveEvent.class, ItemStack.class,
                DataComponentRemoveEvent::getItem, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentRemoveEvent.class, String.class,
                DataComponentRemoveEvent::getComponentName, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentRemoveEvent.class, Object.class,
                DataComponentRemoveEvent::getRemovedValue, EventValues.TIME_PAST);
    }
}
