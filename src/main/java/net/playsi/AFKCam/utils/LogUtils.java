package net.playsi.Afkcam.utils;

import net.playsi.Afkcam.Afkcam;
import net.playsi.Afkcam.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    private final Logger logger;
    private final String className;
    private static final Config CONFIG = Config.getInstance();

    public LogUtils(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(Afkcam.MOD_NAME);
        this.className = clazz.getSimpleName();
    }

    public void info(String message) {
        if (CONFIG.isModEnabled()) {
            logger.info("[{}] {}", className, message);
        }
    }

    public void warn(String message) {
        if (CONFIG.isModEnabled()) {
            logger.warn("[{}] {}", className, message);
        }
    }

    public void error(String message) {
        if (CONFIG.isModEnabled()) {
            logger.error("[{}] {}", className, message);
        }
    }

    public void infoDebug(String message) {
        if (CONFIG.isModEnabled() && CONFIG.isDebugLogEnabled()) {
            logger.info("[{}] [DEBUG] {}", className, message);
        }
    }

    public void warnDebug(String message) {
        if (CONFIG.isModEnabled() && CONFIG.isDebugLogEnabled()) {
            logger.warn("[{}] [DEBUG] {}", className, message);
        }
    }

    public void errorDebug(String message) {
        if (CONFIG.isModEnabled() && CONFIG.isDebugLogEnabled()) {
            logger.error("[{}] [DEBUG] {}", className, message);
        }
    }
}
