package com.venomgrave.hexvg.datacomponents;

import com.venomgrave.hexvg.datacomponents.utils.ComponentLogger;
import com.venomgrave.hexvg.datacomponents.utils.VersionChecker;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class HexVGDataComponents extends JavaPlugin {

    private static HexVGDataComponents instance;
    private SkriptAddon addon;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ComponentLogger.init();

        if (!VersionChecker.isSupported()) {
            ComponentLogger.error("HexVG-DataComponents wymaga Minecraft 1.20.5+! Plugin zostaje wylaczony.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            addon = Skript.registerAddon(this);
            addon.setLanguageFileDirectory("lang");
            addon.loadClasses("com.venomgrave.hexvg.datacomponents", "elements", "events");
            ComponentLogger.info("v" + getDescription().getVersion() + " wlaczony.");
            if (ComponentLogger.isDebugEnabled()) {
                ComponentLogger.debug("Tryb debug jest wlaczony.");
            }
        } catch (Exception e) {
            ComponentLogger.error("Blad podczas inicjalizacji Skript: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        ComponentLogger.info("wylaczony.");
    }

    public static HexVGDataComponents getInstance() {
        return instance;
    }

    public SkriptAddon getAddon() {
        return addon;
    }
}
