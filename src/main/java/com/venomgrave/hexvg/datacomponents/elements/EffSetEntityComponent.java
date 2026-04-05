package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.EntityComponentHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class EffSetEntityComponent extends Effect {

    static {
        Skript.registerEffect(
                EffSetEntityComponent.class,
                "set entity component %string% of %entity% to %strings%",
                "set entity component %string% of %entity% to %number%",
                "set entity component %string% of %entity% to %boolean%"
        );
    }

    private Expression<String> componentName;
    private Expression<Entity> entity;
    private Expression<?> value;
    private int pattern;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        entity        = (Expression<Entity>) exprs[1];
        value         = exprs[2];
        pattern       = matchedPattern;
        return true;
    }

    @Override
    protected void execute(Event event) {
        String name   = componentName.getSingle(event);
        Entity ent    = entity.getSingle(event);
        if (name == null || ent == null) return;

        Object finalValue = resolveValue(event);
        if (finalValue == null) return;

        EntityComponentHandler.write(ent, name, finalValue);
    }

    @SuppressWarnings("unchecked")
    private Object resolveValue(Event event) {
        return switch (pattern) {
            case 0 -> {
                String[] strings = ((Expression<String>) value).getAll(event);
                yield (strings == null || strings.length == 0) ? null
                        : strings.length == 1 ? strings[0] : Arrays.asList(strings);
            }
            case 1 -> (Number) value.getSingle(event);
            case 2 -> (Boolean) value.getSingle(event);
            default -> null;
        };
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set entity component " + componentName.toString(event, debug)
                + " of " + entity.toString(event, debug)
                + " to " + value.toString(event, debug);
    }
}
