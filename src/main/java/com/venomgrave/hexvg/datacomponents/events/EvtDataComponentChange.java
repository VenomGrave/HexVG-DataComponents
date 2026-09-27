package com.venomgrave.hexvg.datacomponents.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import com.venomgrave.hexvg.datacomponents.utils.EventValueRegistrar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class EvtDataComponentChange extends SimpleEvent {

    static {
        Skript.registerEvent(
                "Data Component Change",
                EvtDataComponentChange.class,
                DataComponentChangeEvent.class,
                "data component change"
        );

        EventValueRegistrar.register(DataComponentChangeEvent.class, Player.class,
                DataComponentChangeEvent::getPlayer, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentChangeEvent.class, ItemStack.class,
                DataComponentChangeEvent::getItem, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentChangeEvent.class, String.class,
                DataComponentChangeEvent::getComponentName, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentChangeEvent.class, Object.class,
                DataComponentChangeEvent::getNewValue, EventValues.TIME_NOW);
        EventValueRegistrar.register(DataComponentChangeEvent.class, Object.class,
                DataComponentChangeEvent::getOldValue, EventValues.TIME_PAST);
    }
}
