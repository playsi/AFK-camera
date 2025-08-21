package org.playsi.afkcam.client.Utils;

import org.playsi.afkcam.Afkcam;
import org.playsi.afkcam.client.AfkcamClient;
import org.playsi.afkcam.client.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    private final Logger logger;
    private final String className;
    private static final Config config = AfkcamClient.getConfig();

    public LogUtils(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(Afkcam.MOD_NAME);
        this.className = clazz.getSimpleName();
    }

    public void info(String message) {
        if (config.isModEnabled()) {
            logger.info("[{}] {}", className, message);
        }
    }

    public void warn(String message) {
        if (config.isModEnabled()) {
            logger.warn("[{}] {}", className, message);
        }
    }

    public void error(String message) {
        if (config.isModEnabled()) {
            logger.error("[{}] {}", className, message);
        }
    }

    public void infoDebug(String message) {
        if (config.isModEnabled() && config.isDebugLogEnabled()) {
            logger.info("[{}] [DEBUG] {}", className, message);
        }
    }

    public void warnDebug(String message) {
        if (config.isModEnabled() && config.isDebugLogEnabled()) {
            logger.warn("[{}] [DEBUG] {}", className, message);
        }
    }

    public void errorDebug(String message) {
        if (config.isModEnabled() && config.isDebugLogEnabled()) {
            logger.error("[{}] [DEBUG] {}", className, message);
        }
    }
}
