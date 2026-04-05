package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.EntityComponentHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ExprEntityComponent extends SimpleExpression<Object> {

    static {
        Skript.registerExpression(
                ExprEntityComponent.class,
                Object.class,
                ExpressionType.COMBINED,
                "entity component %string% of %entity%"
        );
    }

    private Expression<String> componentName;
    private Expression<Entity> entity;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        entity        = (Expression<Entity>) exprs[1];
        return true;
    }

    @Override
    protected Object[] get(Event event) {
        String name = componentName.getSingle(event);
        Entity ent  = entity.getSingle(event);
        if (name == null || ent == null) return new Object[0];
        return EntityComponentHandler.read(ent, name)
                .map(val -> new Object[]{val})
                .orElse(new Object[0]);
    }

    @Override
    public Class<?>[] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.SET || mode == Changer.ChangeMode.DELETE) {
            return new Class<?>[]{Object[].class};
        }
        return null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        String name = componentName.getSingle(event);
        Entity ent  = entity.getSingle(event);
        if (name == null || ent == null) return;

        if (mode == Changer.ChangeMode.SET) {
            if (delta == null || delta.length == 0) return;
            Object finalValue = delta.length == 1 ? delta[0] : Arrays.asList(delta);
            EntityComponentHandler.write(ent, name, finalValue);
        } else if (mode == Changer.ChangeMode.DELETE) {
            EntityComponentHandler.remove(ent, name);
        }
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<Object> getReturnType() { return Object.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "entity component " + componentName.toString(event, debug)
                + " of " + entity.toString(event, debug);
    }
}
