package com.venomgrave.hexvg.datacomponents.utils;

import ch.njol.skript.registrations.EventValues;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.converter.Converter;

public final class EventValueRegistrar {

    private EventValueRegistrar() {}

    /**
     * Rejestruje event-value w Skripcie (API Skript 2.10+). Blad pojedynczej rejestracji
     * nie blokuje rejestracji calego eventu.
     */
    public static <E extends Event, T> void register(Class<E> eventClass, Class<T> valueClass,
                                                     Converter<E, T> converter, int time) {
        try {
            EventValues.registerEventValue(eventClass, valueClass, converter, time);
        } catch (Exception | LinkageError e) {
            ComponentLogger.warn("Nie mozna zarejestrowac event-value " + valueClass.getSimpleName()
                    + " dla " + eventClass.getSimpleName() + ": " + e.getMessage());
        }
    }
}
