package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import com.venomgrave.hexvg.datacomponents.utils.ComponentTypeRegistry;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class CondIsValidComponent extends Condition {

    static {
        Skript.registerCondition(
                CondIsValidComponent.class,
                "%string% is a valid data component",
                "%string% is a known data component",
                "%string% is(n't| not) a valid data component",
                "%string% is(n't| not) a known data component"
        );
    }

    private Expression<String> componentName;
    private boolean checkRegistry;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName  = (Expression<String>) exprs[0];
        checkRegistry  = matchedPattern == 1 || matchedPattern == 3;
        setNegated(matchedPattern >= 2);
        return true;
    }

    @Override
    public boolean check(Event event) {
        String name = componentName.getSingle(event);
        if (name == null) return isNegated();
        boolean result = checkRegistry ? ComponentTypeRegistry.isKnown(name) : ComponentConverter.isValidComponentName(name);
        return isNegated() != result;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return componentName.toString(event, debug)
                + (isNegated() ? " is not" : " is")
                + (checkRegistry ? " a known" : " a valid")
                + " data component";
    }
}
