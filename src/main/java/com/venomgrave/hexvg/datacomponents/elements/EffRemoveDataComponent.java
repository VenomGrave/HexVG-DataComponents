package com.venomgrave.hexvg.datacomponents.elements;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import com.venomgrave.hexvg.datacomponents.handlers.ItemComponentHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EffRemoveDataComponent extends Effect {

    static {
        Skript.registerEffect(
                EffRemoveDataComponent.class,
                "remove data component %string% from %itemstack%"
        );
    }

    private Expression<String> componentName;
    private Expression<ItemStack> item;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        componentName = (Expression<String>) exprs[0];
        item = (Expression<ItemStack>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String name = componentName.getSingle(event);
        ItemStack stack = item.getSingle(event);
        if (name == null || stack == null) return;

        Player player = ItemComponentHandler.extractPlayer(event);

        ItemStack modified = stack.clone();
        ItemComponentHandler.remove(modified, name, player);
        item.change(event, new ItemStack[]{modified}, Changer.ChangeMode.SET);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "remove data component " + componentName.toString(event, debug) + " from " + item.toString(event, debug);
    }
}
