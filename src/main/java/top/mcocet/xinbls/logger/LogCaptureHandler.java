package top.mcocet.xinbls.logger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import top.mcocet.xinbls.config.ConfigManager;

/**
 * 日志捕获处理器
 * 使用 Logback 框架来捕获所有日志输出并写入到文件
 */
public class LogCaptureHandler extends AppenderBase<ILoggingEvent> {
    private final LogFileManager logFileManager;
    private final ConfigManager configManager;
    private PatternLayoutEncoder encoder;
    
    public LogCaptureHandler(LogFileManager logFileManager) {
        this.logFileManager = logFileManager;
        this.configManager = logFileManager.getConfigManager(); // 从LogFileManager获取配置管理器
        setName("XinBlsFileAppender");
        setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    }
    
    public void startLogging() {
        // 创建编码器
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} [%level] [%logger{36}] %msg%n");
        encoder.setCharset(java.nio.charset.StandardCharsets.UTF_8);
        encoder.start();
        
        setEncoder(encoder);
        start();
        
        // 将此 Appender 添加到根日志记录器
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(this);
    }
    
    public void stopLogging() {
        if (isStarted()) {
            // 从根日志记录器移除此 Appender
            Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(this);
            stop();
        }
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        // 格式化日志事件并写入文件
        try {
            // 使用 encoder 编码事件
            byte[] encodedEvent = encoder.encode(event);
            String formattedMessage = new String(encodedEvent, java.nio.charset.StandardCharsets.UTF_8);
            
            // 过滤ANSI颜色代码
            String cleanMessage = removeAnsiCodes(formattedMessage.trim());
            
            // 检查是否应该跳过此消息
            if (shouldSkipMessage(cleanMessage)) {
                return; // 跳过写入此消息
            }
            
            logFileManager.writeLogWithoutTimestamp(cleanMessage);
        } catch (Exception e) {
            // 发生错误时，使用备用方法记录
            String fallbackMessage = String.format("%s [%s] [%s] %s",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage()
            );
            
            // 过滤ANSI颜色代码
            String cleanFallbackMessage = removeAnsiCodes(fallbackMessage);
            
            // 检查是否应该跳过此消息
            if (shouldSkipMessage(cleanFallbackMessage)) {
                return; // 跳过写入此消息
            }
            
            logFileManager.writeLogWithoutTimestamp(cleanFallbackMessage);
        }
    }
    
    /**
     * 判断是否应该跳过特定消息
     * @param message 要检查的消息
     * @return 如果应该跳过返回true，否则返回false
     */
    private boolean shouldSkipMessage(String message) {
        if (message == null) {
            return false;
        }
        
        // 检查是否需要跳过包含“来自”的消息
        if (configManager.isSkipMessagesWithFrom() && message.contains("来自")) {
            return true;
        }
        
        // 检查是否需要跳过包含“发至”的消息
        if (configManager.isSkipMessagesWithTo() && message.contains("发至")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 移除ANSI颜色代码
     * @param input 包含ANSI颜色代码的字符串
     * @return 清理后的字符串
     */
    private String removeAnsiCodes(String input) {
        if (input == null) {
            return null;
        }
        
        // ANSI转义序列的正则表达式
        // \u001B\[[0-9;]*m 匹配ESC[数字;数字;...m这样的序列
        // \u009B[0-9;]*m 匹配CSI(控制序列介绍符)序列
        return input.replaceAll("\\u001B\\[[0-9;]*m", "")
                   .replaceAll("\\u009B[0-9;]*m", "");
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }
}