package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.events.DataComponentChangeEvent;
import com.venomgrave.hexvg.datacomponents.events.DataComponentRemoveEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class EffCancelComponentChange extends Effect {

    static {
        Skript.registerEffect(
                EffCancelComponentChange.class,
                "cancel [the] [data] component (change|modification)",
                "cancel [the] [data] component removal"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (matchedPattern == 0 && !getParser().isCurrentEvent(DataComponentChangeEvent.class)) {
            Skript.error("'cancel component change' mozna uzywac tylko w evencie 'data component change'.");
            return false;
        }
        if (matchedPattern == 1 && !getParser().isCurrentEvent(DataComponentRemoveEvent.class)) {
            Skript.error("'cancel component removal' mozna uzywac tylko w evencie 'data component remove'.");
            return false;
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (event instanceof DataComponentChangeEvent e) e.setCancelled(true);
        else if (event instanceof DataComponentRemoveEvent e) e.setCancelled(true);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "cancel component change";
    }
}
