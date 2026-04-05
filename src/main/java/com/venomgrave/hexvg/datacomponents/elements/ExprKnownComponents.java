package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.utils.ComponentTypeRegistry;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprKnownComponents extends SimpleExpression<String> {

    static {
        Skript.registerExpression(
                ExprKnownComponents.class,
                String.class,
                ExpressionType.SIMPLE,
                "all known data components",
                "all registered data components"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event event) {
        return ComponentTypeRegistry.getAllComponentNames().toArray(String[]::new);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<String> getReturnType() { return String.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "all known data components";
    }
}
