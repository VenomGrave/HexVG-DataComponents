package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.events.DataComponentChangeEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprEventComponentNewValue extends SimpleExpression<Object> {

    static {
        Skript.registerExpression(
                ExprEventComponentNewValue.class,
                Object.class,
                ExpressionType.SIMPLE,
                "[the] [event-]new component value",
                "[the] [event-]component value"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        if (!getParser().isCurrentEvent(DataComponentChangeEvent.class)) {
            Skript.error("'new component value' mozna uzywac tylko w evencie 'data component change'.");
            return false;
        }
        return true;
    }

    @Override
    @Nullable
    protected Object[] get(Event event) {
        if (!(event instanceof DataComponentChangeEvent e)) return new Object[0];
        Object val = e.getNewValue();
        if (val == null) return new Object[0];
        return new Object[]{val};
    }

    @Override
    @Nullable
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET) return new Class<?>[]{Object.class};
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (!(event instanceof DataComponentChangeEvent e)) return;
        if (mode == Changer.ChangeMode.SET && delta != null && delta.length > 0) e.setNewValue(delta[0]);
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<Object> getReturnType() { return Object.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) { return "new component value"; }
}
