package com.venomgrave.hexvg.datacomponents.utils;

import com.venomgrave.hexvg.datacomponents.HexVGDataComponents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ComponentLogger {

    private static final String PREFIX = "[HexVG-DC] ";

    // Ochrona przed zalewaniem konsoli: identyczne ostrzezenie wypisujemy maks. raz na WARN_COOLDOWN_MS.
    // Skrypt wykonywany co tick z blednymi danymi (np. od gracza) nie zapcha logow ani dysku.
    private static final long WARN_COOLDOWN_MS = 10_000L;
    private static final int MAX_TRACKED_WARNINGS = 512;
    private static final Map<String, Long> LAST_WARNINGS = new ConcurrentHashMap<>();

    private static volatile Logger logger = Logger.getLogger("HexVG-DataComponents");
    private static volatile boolean debugEnabled = false;

    private ComponentLogger() {}

    public static void init() {
        HexVGDataComponents plugin = HexVGDataComponents.getInstance();
        logger = plugin.getLogger();
        debugEnabled = plugin.getConfig().getBoolean("debug", false);
        LAST_WARNINGS.clear();
    }

    public static boolean isDebugEnabled() { return debugEnabled; }

    public static void info(String message)  { logger.info(PREFIX + message); }
    public static void error(String message) { logger.severe(PREFIX + message); }

    public static void warn(String message) {
        if (!debugEnabled && isThrottled(message)) return;
        logger.warning(PREFIX + message);
    }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, PREFIX + message, throwable);
    }

    public static void debug(String message) {
        if (debugEnabled) logger.info(PREFIX + "[DEBUG] " + message);
    }

    private static boolean isThrottled(String message) {
        long now = System.currentTimeMillis();
        if (LAST_WARNINGS.size() > MAX_TRACKED_WARNINGS) LAST_WARNINGS.clear();
        Long last = LAST_WARNINGS.put(message, now);
        return last != null && now - last < WARN_COOLDOWN_MS;
    }
}
