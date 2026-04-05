package com.venomgrave.hexvg.datacomponents.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EvtDataComponentChange extends SimpleEvent {

    static {
        Skript.registerEvent(
                "Data Component Change",
                EvtDataComponentChange.class,
                DataComponentChangeEvent.class,
                "data component change"
        );

        registerSafe(DataComponentChangeEvent.class, Player.class,
                new Getter<Player, DataComponentChangeEvent>() {
                    @Override @Nullable public Player get(DataComponentChangeEvent e) { return e.getPlayer(); }
                }, EventValues.TIME_NOW);

        registerSafe(DataComponentChangeEvent.class, ItemStack.class,
                new Getter<ItemStack, DataComponentChangeEvent>() {
                    @Override public ItemStack get(DataComponentChangeEvent e) { return e.getItem(); }
                }, EventValues.TIME_NOW);

        registerSafe(DataComponentChangeEvent.class, String.class,
                new Getter<String, DataComponentChangeEvent>() {
                    @Override public String get(DataComponentChangeEvent e) { return e.getComponentName(); }
                }, EventValues.TIME_NOW);

        registerSafe(DataComponentChangeEvent.class, Object.class,
                new Getter<Object, DataComponentChangeEvent>() {
                    @Override @Nullable public Object get(DataComponentChangeEvent e) { return e.getNewValue(); }
                }, EventValues.TIME_NOW);

        registerSafe(DataComponentChangeEvent.class, Object.class,
                new Getter<Object, DataComponentChangeEvent>() {
                    @Override @Nullable public Object get(DataComponentChangeEvent e) { return e.getOldValue(); }
                }, EventValues.TIME_PAST);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T, E> void registerSafe(Class<E> eventClass, Class<T> valueClass,
                                             Getter<T, E> getter, int time) {
        try {
            
            EventValues.class
                .getMethod("registerEventValue", Class.class, Class.class, Getter.class, int.class, String.class, Class[].class)
                .invoke(null, eventClass, valueClass, getter, time, null, (Object) new Class[0]);
        } catch (NoSuchMethodException ex) {
            try {
                
                EventValues.class
                    .getMethod("registerEventValue", Class.class, Class.class, Getter.class, int.class)
                    .invoke(null, eventClass, valueClass, getter, time);
            } catch (Exception ex2) {
                Skript.warning("[HexVG-DC] Nie mozna zarejestrowac EventValue dla " + valueClass.getSimpleName() + ": " + ex2.getMessage());
            }
        } catch (Exception ex) {
            Skript.warning("[HexVG-DC] Blad rejestracji EventValue dla " + valueClass.getSimpleName() + ": " + ex.getMessage());
        }
    }
}
