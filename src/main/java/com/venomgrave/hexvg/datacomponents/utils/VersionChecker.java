package com.venomgrave.hexvg.datacomponents.utils;

import org.bukkit.Bukkit;

public final class VersionChecker {

    // Minimalna wspierana wersja to Minecraft 1.21.4 (Paper).
    // Data Components API Papera pojawilo sie w 1.21.3, a plugin korzysta z API 1.21.4
    // (m.in. CustomModelData z lista floatow, Attribute.MAX_HEALTH).
    // Stara numeracja: 1.MINOR.PATCH (major == 1).
    // Nowa numeracja (od 2026): major rosnie (np. 26.1.2) i jest zawsze nowszy niz 1.x.
    private static final int LEGACY_MAJOR = 1;
    private static final int MIN_MINOR = 21;
    private static final int MIN_PATCH = 4;

    public static final String MIN_VERSION = "1.21.4";

    private static final String PAPER_API_CLASS = "io.papermc.paper.datacomponent.DataComponentTypes";

    private VersionChecker() {}

    /** Czy serwer udostepnia Data Components API Papera (brak na Spigot/Bukkit). */
    public static boolean hasDataComponentApi() {
        try {
            Class.forName(PAPER_API_CLASS, false, VersionChecker.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static boolean isSupported() {
        if (!hasDataComponentApi()) return false;
        try {
            String raw = Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = raw.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            // Nowa numeracja Minecrafta (major > 1) jest zawsze nowsza niz 1.21.4.
            if (major > LEGACY_MAJOR) return true;
            if (major < LEGACY_MAJOR) return false;

            // Stara numeracja 1.x.y
            if (minor > MIN_MINOR) return true;
            return minor == MIN_MINOR && patch >= MIN_PATCH;
        } catch (Exception e) {
            // Nietypowy format wersji - API jest obecne, wiec probujemy dzialac.
            return true;
        }
    }
}
