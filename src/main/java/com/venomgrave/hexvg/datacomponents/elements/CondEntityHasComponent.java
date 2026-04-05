package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.EntityComponentHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CondEntityHasComponent extends Condition {

    static {
        Skript.registerCondition(
                CondEntityHasComponent.class,
                "%entity% has entity component %string%",
                "%entity% doesn't have entity component %string%"
        );
    }

    private Expression<Entity> entity;
    private Expression<String> componentName;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        entity        = (Expression<Entity>) exprs[0];
        componentName = (Expression<String>) exprs[1];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Entity ent  = entity.getSingle(event);
        String name = componentName.getSingle(event);
        if (ent == null || name == null) return isNegated();
        boolean has = EntityComponentHandler.read(ent, name).isPresent();
        return isNegated() != has;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return entity.toString(event, debug)
                + (isNegated() ? " doesn't have" : " has")
                + " entity component " + componentName.toString(event, debug);
    }
}
