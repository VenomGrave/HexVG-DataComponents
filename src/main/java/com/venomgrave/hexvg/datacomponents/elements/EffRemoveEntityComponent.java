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

public class EffRemoveEntityComponent extends Effect {

    static {
        Skript.registerEffect(
                EffRemoveEntityComponent.class,
                "remove entity component %string% from %entity%",
                "reset entity component %string% of %entity%"
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
    protected void execute(Event event) {
        String name = componentName.getSingle(event);
        Entity ent  = entity.getSingle(event);
        if (name == null || ent == null) return;
        EntityComponentHandler.remove(ent, name);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove entity component " + componentName.toString(event, debug)
                + " from " + entity.toString(event, debug);
    }
}
