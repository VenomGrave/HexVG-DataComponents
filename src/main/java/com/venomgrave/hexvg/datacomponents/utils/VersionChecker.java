package com.venomgrave.hexvg.datacomponents.utils;

import org.bukkit.Bukkit;

public final class VersionChecker {

    private static final int MIN_MINOR = 20;
    private static final int MIN_PATCH = 5;

    private VersionChecker() {}

    public static boolean isSupported() {
        try {
            String raw = Bukkit.getBukkitVersion().split("-")[0];
            String[] parts = raw.split("\\.");
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            if (minor > MIN_MINOR) return true;
            if (minor == MIN_MINOR && patch >= MIN_PATCH) return true;
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
