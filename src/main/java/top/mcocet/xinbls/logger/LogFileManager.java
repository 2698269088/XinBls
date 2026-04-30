package top.mcocet.xinbls.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcocet.xinbls.config.ConfigManager;
import xin.bbtt.mcbot.LangManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志文件管理器
 * 负责创建、管理和轮转日志文件
 */
public class LogFileManager {
    private static final String LOG_DIR = "./plugin/XinBls/";
    private static final String LOG_FILE_PREFIX = "xinbls-";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final Pattern LOG_FILE_PATTERN = Pattern.compile(LOG_FILE_PREFIX + "(\\d{4}-\\d{2}-\\d{2}-\\d{6})" + LOG_FILE_SUFFIX);
    
    private PrintWriter currentWriter;
    private File currentLogFile;
    private int currentLineCount = 0;
    private final Object lock = new Object();
    
    private static final Logger logger = LoggerFactory.getLogger(LogFileManager.class);
    
    private ConfigManager configManager;

    public LogFileManager() {
        this.configManager = new ConfigManager(LOG_DIR);
        this.configManager.loadConfig();
        initializeLogDirectory();
        initializeCurrentLogFile();
    }

    /**
     * 初始化日志目录
     */
    private void initializeLogDirectory() {
        Path logDirPath = Paths.get(LOG_DIR);
        try {
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
                logger.info(LangManager.get("xinbls.log.dir.created", LOG_DIR));
            }
        } catch (Exception e) {
            logger.error(LangManager.get("xinbls.log.dir.failed", e.getMessage()));
        }
    }

    /**
     * 初始化当前日志文件
     * 查找最新的日志文件或创建新的
     */
    private void initializeCurrentLogFile() {
        synchronized (lock) {
            File logDir = new File(LOG_DIR);
            File[] logFiles = logDir.listFiles((dir, name) -> 
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

            if (logFiles != null && logFiles.length > 0) {
                // 找到最新的日志文件
                File latestLogFile = findLatestLogFile(logFiles);
                
                if (latestLogFile != null) {
                    this.currentLogFile = latestLogFile;
                    this.currentLineCount = countLines(latestLogFile);
                    
                    // 如果当前文件行数已经超过限制，则创建新文件
                    if (currentLineCount >= configManager.getMaxLogLines()) {
                        switchToNewLogFile();
                    } else {
                        openCurrentLogFile();
                    }
                } else {
                    switchToNewLogFile();
                }
            } else {
                switchToNewLogFile();
            }
        }
    }

    /**
     * 查找最新的日志文件
     */
    private File findLatestLogFile(File[] logFiles) {
        File latestFile = null;
        LocalDateTime latestDateTime = null;

        for (File file : logFiles) {
            Matcher matcher = LOG_FILE_PATTERN.matcher(file.getName());
            if (matcher.matches()) {
                try {
                    String dateTimeStr = matcher.group(1); // 格式: yyyy-MM-dd-HHmmss
                    LocalDateTime fileDateTime = LocalDateTime.parse(dateTimeStr, 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
                    
                    if (latestDateTime == null || fileDateTime.isAfter(latestDateTime)) {
                        latestDateTime = fileDateTime;
                        latestFile = file;
                    }
                } catch (Exception e) {
                    logger.warn(LangManager.get("xinbls.log.file.parse.failed", file.getName(), e.getMessage()));
                }
            }
        }

        return latestFile;
    }

    /**
     * 创建新的日志文件
     */
    private File createNewLogFile() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        String fileName = LOG_FILE_PREFIX + timestamp + LOG_FILE_SUFFIX;
        File newLogFile = new File(LOG_DIR, fileName);

        logger.info(LangManager.get("xinbls.log.file.created", newLogFile.getAbsolutePath()));
        return newLogFile;
    }

    /**
     * 打开当前日志文件的写入流
     */
    private void openCurrentLogFile() {
        try {
            if (currentWriter != null) {
                currentWriter.close();
            }
            
            // 使用正确的字符编码创建 PrintWriter
            currentWriter = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(currentLogFile, true), // 追加模式
                    StandardCharsets.UTF_8
                ), 
                true // auto-flush
            );
        } catch (Exception e) {
            logger.error(LangManager.get("xinbls.log.file.open.failed", e.getMessage()));
        }
    }

    /**
     * 计算文件行数
     */
    private int countLines(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            long count = reader.lines().count();
            return (int) count;
        } catch (Exception e) {
            logger.warn(LangManager.get("xinbls.log.line.count.failed", e.getMessage()));
            return 0;
        }
    }

    /**
     * 写入带时间戳的日志
     */
    public void writeLog(String logMessage) {
        synchronized (lock) {
            // 检查是否需要跳过特定消息
            if (shouldSkipMessage(logMessage)) {
                return; // 跳过写入此消息
            }
            
            // 检查是否需要切换到新文件
            if (currentLineCount >= configManager.getMaxLogLines()) {
                switchToNewLogFile();
            }

            try {
                if (currentWriter != null) {
                    String cleanMessage = removeAnsiCodes(logMessage);
                    String formattedMessage = formatLogEntry(cleanMessage);
                    currentWriter.println(formattedMessage);
                    currentWriter.flush(); // 立即写入磁盘
                    currentLineCount++;
                }
            } catch (Exception e) {
                logger.error(LangManager.get("xinbls.log.write.failed", e.getMessage()));
            }
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
        
        // 检查是否需要跳过包含"来自"的消息
        if (configManager.isSkipMessagesWithFrom() && message.contains("来自")) {
            return true;
        }
        
        // 检查是否需要跳过包含"发至"的消息
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

    /**
     * 切换到新的日志文件
     */
    private void switchToNewLogFile() {
        try {
            if (currentWriter != null) {
                currentWriter.close();
            }
        } catch (Exception e) {
            logger.error(LangManager.get("xinbls.log.file.close.failed", e.getMessage()));
        }

        this.currentLogFile = createNewLogFile();
        this.currentLineCount = 0;
        openCurrentLogFile();
        
        // 在创建新文件后检查是否需要清理旧日志文件
        if (configManager.isAutoCleanEnabled()) {
            cleanupOldLogFiles();
        }
    }

    /**
     * 清理旧的日志文件
     */
    private void cleanupOldLogFiles() {
        File logDir = new File(LOG_DIR);
        File[] logFiles = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));

        if (logFiles == null || logFiles.length <= configManager.getMaxLogFileCount()) {
            // 如果文件数量没有超过限制，则无需清理
            return;
        }

        // 按文件创建时间排序（最旧的在前）
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));

        // 删除超出数量限制的最旧文件
        int filesToDeleteCount = logFiles.length - configManager.getMaxLogFileCount();
        for (int i = 0; i < filesToDeleteCount; i++) {
            try {
                if (!logFiles[i].equals(currentLogFile)) { // 确保不删除当前正在使用的文件
                    boolean deleted = logFiles[i].delete();
                    if (deleted) {
                        logger.info(LangManager.get("xinbls.log.file.deleted", logFiles[i].getName()));
                    } else {
                        logger.warn(LangManager.get("xinbls.log.file.delete.failed", logFiles[i].getName()));
                    }
                }
            } catch (SecurityException e) {
                logger.error(LangManager.get("xinbls.log.file.delete.security", logFiles[i].getName()), e);
            }
        }
    }

    /**
     * 格式化日志条目
     */
    private String formatLogEntry(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        return "[" + timestamp + "] " + message;
    }

    /**
     * 获取当前日志文件
     */
    public File getCurrentLogFile() {
        return currentLogFile;
    }

    /**
     * 关闭日志文件管理器
     */
    public void close() {
        synchronized (lock) {
            if (currentWriter != null) {
                currentWriter.close();
                currentWriter = null;
            }
        }
    }
    
    /**
     * 获取配置管理器
     * @return 配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
