package top.mcocet.xinbls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xin.bbtt.mcbot.Bot;
import xin.bbtt.mcbot.LangManager;
import xin.bbtt.mcbot.plugin.Plugin;

import top.mcocet.xinbls.config.ConfigManager;
import top.mcocet.xinbls.logger.LogFileManager;

public class XinBls implements Plugin {
    private LogFileManager logFileManager;
    private XinBlsListener listener;
    private static final Logger logger = LoggerFactory.getLogger(XinBls.class);

    @Override
    public void onLoad() {
        LangManager.initLang(this.getClass().getClassLoader());
        logger.info(LangManager.get("xinbls.load"));
    }

    @Override
    public void onEnable() {
        try {
            logFileManager = new LogFileManager();
            listener = new XinBlsListener(logFileManager);
            Bot.INSTANCE.getPluginManager().registerEvents(listener, this);
            logger.info(LangManager.get("xinbls.enabled", logFileManager.getCurrentLogFile().getAbsolutePath()));
        } catch (Exception e) {
            logger.error(LangManager.get("xinbls.enable.failed", e.getMessage()), e);
        }
    }

    @Override
    public void onDisable() {
        logger.info(LangManager.get("xinbls.disable"));
        try {
            if (listener != null) {
                Bot.INSTANCE.getPluginManager().events().unregisterAll(this);
                listener = null;
            }
            if (logFileManager != null) {
                logFileManager.close();
                logFileManager = null;
            }
        } catch (Exception e) {
            logger.error(LangManager.get("xinbls.disable.failed", e.getMessage()), e);
        }
    }

    @Override
    public void onUnload() {
        logger.info(LangManager.get("xinbls.unload"));
    }
}
