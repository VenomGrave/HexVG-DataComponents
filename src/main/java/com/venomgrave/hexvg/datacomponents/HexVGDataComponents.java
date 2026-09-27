package com.venomgrave.hexvg.datacomponents;

import com.venomgrave.hexvg.datacomponents.handlers.EntityComponentHandler;
import com.venomgrave.hexvg.datacomponents.utils.ComponentConverter;
import com.venomgrave.hexvg.datacomponents.utils.ComponentLogger;
import com.venomgrave.hexvg.datacomponents.utils.ComponentTypeRegistry;
import com.venomgrave.hexvg.datacomponents.utils.VersionChecker;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

public final class HexVGDataComponents extends JavaPlugin {

    private static HexVGDataComponents instance;
    private SkriptAddon addon;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ComponentLogger.init();
        ComponentConverter.configureLimits(
                getConfig().getInt("limits.max-string-length", ComponentConverter.DEFAULT_MAX_STRING_LENGTH),
                getConfig().getInt("limits.max-list-size", ComponentConverter.DEFAULT_MAX_LIST_SIZE));

        if (!VersionChecker.isSupported()) {
            ComponentLogger.error("HexVG-DataComponents wymaga serwera Paper " + VersionChecker.MIN_VERSION
                    + "+ (Data Components API)! Plugin zostaje wylaczony.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!Skript.isAcceptRegistrations()) {
            ComponentLogger.error("Skript nie przyjmuje juz rejestracji (plugin zaladowany po starcie serwera, np. przez /reload lub PlugMan). "
                    + "Uruchom serwer ponownie.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            ComponentTypeRegistry.initialize();
            addon = Skript.registerAddon(this);
            addon.setLanguageFileDirectory("lang");
            addon.loadClasses("com.venomgrave.hexvg.datacomponents", "elements", "events");
            printBanner();
        } catch (Exception e) {
            ComponentLogger.error("Blad podczas inicjalizacji Skript: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void printBanner() {
        String RESET  = "\u001B[0m";
        String BOLD   = "\u001B[1m";
        String CYAN   = "\u001B[36m";
        String BCYAN  = "\u001B[96m";
        String GRAY   = "\u001B[90m";
        String WHITE  = "\u001B[97m";
        String GREEN  = "\u001B[92m";
        String YELLOW = "\u001B[93m";

        String version = getDescription().getVersion();
        String mc      = getServer().getMinecraftVersion();
        String skript  = Skript.getVersion().toString();
        boolean debug  = ComponentLogger.isDebugEnabled();

        String bar  = GRAY + "+==========" + CYAN + "[ " + BOLD + BCYAN + "HexVG-DataComponents" + RESET + CYAN + " ]" + GRAY + "==========" + RESET;
        String bar2 = GRAY + "+============================================+" + RESET;

        getLogger().info(bar);
        getLogger().info(GRAY + "  Wersja:     " + WHITE + version + RESET);
        getLogger().info(GRAY + "  Minecraft:  " + WHITE + mc + RESET);
        getLogger().info(GRAY + "  Skript:     " + WHITE + skript + RESET);
        getLogger().info(bar2);
        getLogger().info(GRAY + "  Komponenty: " + WHITE + ComponentTypeRegistry.size() + " item / "
                + EntityComponentHandler.SUPPORTED_COMPONENTS.size() + " encja" + RESET);
        getLogger().info(GRAY + "  Debug:      " + (debug ? YELLOW + "wlaczony" : GREEN + "wylaczony") + RESET);
        getLogger().info(bar2);
        getLogger().info("  " + GREEN + BOLD + "Plugin uruchomiony pomyslnie." + RESET);
        getLogger().info(bar);
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
