package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.events.DataComponentChangeEvent;
import com.venomgrave.hexvg.datacomponents.events.DataComponentRemoveEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventComponentName extends SimpleExpression<String> {

    static {
        Skript.registerExpression(
                ExprEventComponentName.class,
                String.class,
                ExpressionType.SIMPLE,
                "[the] [event-]component name",
                "[the] [event-]data component name"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!getParser().isCurrentEvent(DataComponentChangeEvent.class)
                && !getParser().isCurrentEvent(DataComponentRemoveEvent.class)) {
            Skript.error("'component name' mozna uzywac tylko w evencie 'data component change' lub 'data component remove'.");
            return false;
        }
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        if (event instanceof DataComponentChangeEvent e) return new String[]{e.getComponentName()};
        if (event instanceof DataComponentRemoveEvent e) return new String[]{e.getComponentName()};
        return new String[0];
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<String> getReturnType() { return String.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) { return "event component name"; }
}
