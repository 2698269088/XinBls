package top.mcocet.xinbls;

import xin.bbtt.mcbot.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.mcocet.xinbls.logger.LogFileManager;
import top.mcocet.xinbls.logger.LogCaptureHandler;

public class XinBls implements Plugin {
    private LogFileManager logFileManager;
    private LogCaptureHandler logCaptureHandler;
    private static final Logger logger = LoggerFactory.getLogger(XinBls.class);

    @Override
    public String getName() {
        return "XinBls";
        // 返回插件名称
    }

    @Override
    public String getVersion() {
        return "1.1";
        // 返回插件版本
    }

    @Override
    public void onLoad() {
        // 插件被加载时调用
        logger.info("XinBls 插件正在加载...");
        try {
            // 初始化日志文件管理器
            logFileManager = new LogFileManager();
            
            // 创建并启动日志捕获处理器
            logCaptureHandler = new LogCaptureHandler(logFileManager);
            logCaptureHandler.startLogging();

            logger.info("日志记录器已初始化，日志文件: " + logFileManager.getCurrentLogFile().getAbsolutePath());
        } catch (Exception e) {
            logger.error("初始化日志记录器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onUnload() {
        // 插件被卸载时调用
        logger.info("XinBls 插件正在卸载...");
        try {
            // 停止日志捕获
            if (logCaptureHandler != null) {
                logCaptureHandler.stopLogging();
                logCaptureHandler = null;
            }
            
            // 关闭日志文件管理器
            if (logFileManager != null) {
                logFileManager.close();
                logFileManager = null;
            }
        } catch (Exception e) {
            logger.error("清理日志记录器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        // 插件被启用时调用
        // 在这里注册监听器与命令
        logger.info("XinBls 插件已启用");
    }

    @Override
    public void onDisable() {
        // 插件被禁用时调用
        logger.info("XinBls 插件已禁用");
    }
}