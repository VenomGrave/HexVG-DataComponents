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

public class ExprEventComponentOldValue extends SimpleExpression<Object> {

    static {
        Skript.registerExpression(
                ExprEventComponentOldValue.class,
                Object.class,
                ExpressionType.SIMPLE,
                "[the] [event-]old component value",
                "[the] [event-]previous component value"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!getParser().isCurrentEvent(DataComponentChangeEvent.class)
                && !getParser().isCurrentEvent(DataComponentRemoveEvent.class)) {
            Skript.error("'old component value' mozna uzywac tylko w evencie 'data component change' lub 'data component remove'.");
            return false;
        }
        return true;
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        Object val = null;
        if (event instanceof DataComponentChangeEvent e) val = e.getOldValue();
        else if (event instanceof DataComponentRemoveEvent e) val = e.getRemovedValue();
        if (val == null) return new Object[0];
        return new Object[]{val};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<Object> getReturnType() { return Object.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) { return "old component value"; }
}
