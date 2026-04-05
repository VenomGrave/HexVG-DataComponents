package com.venomgrave.hexvg.datacomponents.utils;

import com.venomgrave.hexvg.datacomponents.HexVGDataComponents;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ComponentLogger {

    private static final String PREFIX = "[HexVG-DC] ";
    private static Logger logger = Logger.getLogger("HexVG-DataComponents");
    private static boolean debugEnabled = false;

    private ComponentLogger() {}

    public static void init() {
        logger = HexVGDataComponents.getInstance().getLogger();
        debugEnabled = HexVGDataComponents.getInstance().getConfig().getBoolean("debug", false);
    }

    public static boolean isDebugEnabled() { return debugEnabled; }

    public static void info(String message)  { logger.info(PREFIX + message); }
    public static void warn(String message)  { logger.warning(PREFIX + message); }
    public static void error(String message) { logger.severe(PREFIX + message); }

    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, PREFIX + message, throwable);
    }

    public static void debug(String message) {
        if (debugEnabled) logger.info(PREFIX + "[DEBUG] " + message);
    }
}
