package top.mcocet.xinbls.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志文件管理器
 * 负责创建、管理和轮转日志文件
 */
public class LogFileManager {
    private static final String LOG_DIR = "./plugin/XinBls/";
    private static final int MAX_LINES = 10000;
    private static final String LOG_FILE_PREFIX = "xinbls-";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final Pattern LOG_FILE_PATTERN = Pattern.compile(LOG_FILE_PREFIX + "(\\d{4}-\\d{2}-\\d{2}-\\d{6})" + LOG_FILE_SUFFIX);
    
    private PrintWriter currentWriter;
    private File currentLogFile;
    private int currentLineCount = 0;
    private final Object lock = new Object();
    
    private static final Logger logger = LoggerFactory.getLogger(LogFileManager.class);

    public LogFileManager() {
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
                logger.info("创建日志目录: " + LOG_DIR);
            }
        } catch (Exception e) {
            logger.error("创建日志目录失败: " + e.getMessage());
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
                    if (currentLineCount >= MAX_LINES) {
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
                    logger.warn("解析日志文件名失败: " + file.getName() + ", 错误: " + e.getMessage());
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

        logger.info("创建新的日志文件: " + newLogFile.getAbsolutePath());
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
            logger.error("打开日志文件失败: " + e.getMessage());
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
            logger.warn("计算文件行数失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 写入带时间戳的日志
     */
    public void writeLog(String logMessage) {
        synchronized (lock) {
            // 检查是否需要切换到新文件
            if (currentLineCount >= MAX_LINES) {
                switchToNewLogFile();
            }

            try {
                if (currentWriter != null) {
                    String formattedMessage = formatLogEntry(logMessage);
                    currentWriter.println(formattedMessage);
                    currentWriter.flush(); // 立即写入磁盘
                    currentLineCount++;
                }
            } catch (Exception e) {
                logger.error("写入日志失败: " + e.getMessage());
            }
        }
    }

    /**
     * 写入不带时间戳的日志（用于Logback捕获）
     */
    public void writeLogWithoutTimestamp(String logMessage) {
        synchronized (lock) {
            // 检查是否需要切换到新文件
            if (currentLineCount >= MAX_LINES) {
                switchToNewLogFile();
            }

            try {
                if (currentWriter != null && logMessage != null && !logMessage.isEmpty()) {
                    currentWriter.println(logMessage);
                    currentWriter.flush(); // 立即写入磁盘
                    currentLineCount++;
                }
            } catch (Exception e) {
                logger.error("写入日志失败: " + e.getMessage());
            }
        }
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
            logger.error("关闭旧日志文件失败: " + e.getMessage());
        }

        this.currentLogFile = createNewLogFile();
        this.currentLineCount = 0;
        openCurrentLogFile();
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
}