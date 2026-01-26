package top.mcocet.xinbls.logger;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

/**
 * 日志捕获处理器
 * 使用 Logback 框架来捕获所有日志输出并写入到文件
 */
public class LogCaptureHandler extends AppenderBase<ILoggingEvent> {
    private final LogFileManager logFileManager;
    private PatternLayoutEncoder encoder;
    
    public LogCaptureHandler(LogFileManager logFileManager) {
        this.logFileManager = logFileManager;
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
            logFileManager.writeLogWithoutTimestamp(formattedMessage.trim());
        } catch (Exception e) {
            // 发生错误时，使用备用方法记录
            String fallbackMessage = String.format("%s [%s] [%s] %s",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage()
            );
            logFileManager.writeLogWithoutTimestamp(fallbackMessage);
        }
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }
}