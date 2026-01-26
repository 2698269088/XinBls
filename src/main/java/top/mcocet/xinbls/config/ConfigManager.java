package top.mcocet.xinbls.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.mcocet.xinbls.logger.LogFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 配置管理器
 * 负责读取、保存和管理插件配置
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String DEFAULT_LOG_DIR = "./plugin/XinBls/";
    
    private transient String configPath; // 不序列化此字段
    
    // 配置属性
    private int maxLogLines;                           // 日志文件最大行数，超过此数值创建新文件
    private boolean autoCleanEnabled;                  // 是否自动清理日志文件
    private int maxLogFileCount;                       // 最大日志文件数量，超过此数量会清理旧文件
    private boolean skipMessagesWithFrom;              // 是否跳过包含“来自”的信息
    private boolean skipMessagesWithTo;                // 是否跳过包含“发至”的信息
    private boolean skipMessagesWithBracketPrefix;     // 是否跳过包含字母数字中括号前缀的关键字消息
    
    // 默认值
    private static final int DEFAULT_MAX_LOG_LINES = 20000;
    private static final boolean DEFAULT_AUTO_CLEAN_ENABLED = true;
    private static final int DEFAULT_MAX_LOG_FILE_COUNT = 10;
    private static final boolean DEFAULT_SKIP_MESSAGES_WITH_FROM = false;
    private static final boolean DEFAULT_SKIP_MESSAGES_WITH_TO = false;
    private static final boolean DEFAULT_SKIP_MESSAGES_WITH_BRACKET_PREFIX = false;
    
    public ConfigManager() {
        // 设置默认值
        this.maxLogLines = DEFAULT_MAX_LOG_LINES;
        this.autoCleanEnabled = DEFAULT_AUTO_CLEAN_ENABLED;
        this.maxLogFileCount = DEFAULT_MAX_LOG_FILE_COUNT;
        this.skipMessagesWithFrom = DEFAULT_SKIP_MESSAGES_WITH_FROM;
        this.skipMessagesWithTo = DEFAULT_SKIP_MESSAGES_WITH_TO;
        this.skipMessagesWithBracketPrefix = DEFAULT_SKIP_MESSAGES_WITH_BRACKET_PREFIX;
        
        // 设置配置文件路径
        this.configPath = DEFAULT_LOG_DIR + CONFIG_FILE_NAME;
    }
    
    /**
     * 带自定义日志目录的构造函数
     */
    public ConfigManager(String logDir) {
        // 设置默认值
        this.maxLogLines = DEFAULT_MAX_LOG_LINES;
        this.autoCleanEnabled = DEFAULT_AUTO_CLEAN_ENABLED;
        this.maxLogFileCount = DEFAULT_MAX_LOG_FILE_COUNT;
        this.skipMessagesWithFrom = DEFAULT_SKIP_MESSAGES_WITH_FROM;
        this.skipMessagesWithTo = DEFAULT_SKIP_MESSAGES_WITH_TO;
        this.skipMessagesWithBracketPrefix = DEFAULT_SKIP_MESSAGES_WITH_BRACKET_PREFIX;
        
        // 设置配置文件路径
        this.configPath = logDir + "/" + CONFIG_FILE_NAME;
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        File configFile = new File(configPath);
        
        if (!configFile.exists()) {
            logger.info("配置文件不存在，使用默认配置: " + configPath);
            saveConfig(); // 保存默认配置到文件
            return;
        }
        
        try {
            String content = new String(Files.readAllBytes(Paths.get(configPath)));
            Gson gson = new Gson();
            ConfigManager loadedConfig = gson.fromJson(content, ConfigManager.class);
            
            // 更新当前配置对象的值
            this.maxLogLines = loadedConfig.maxLogLines;
            this.autoCleanEnabled = loadedConfig.autoCleanEnabled;
            this.maxLogFileCount = loadedConfig.maxLogFileCount;
            this.skipMessagesWithFrom = loadedConfig.skipMessagesWithFrom;
            this.skipMessagesWithTo = loadedConfig.skipMessagesWithTo;
            this.skipMessagesWithBracketPrefix = loadedConfig.skipMessagesWithBracketPrefix;
            
            logger.info("配置加载成功: " + configPath);
        } catch (IOException e) {
            logger.error("读取配置文件失败: " + configPath, e);
            logger.info("使用默认配置");
        } catch (Exception e) {
            logger.error("解析配置文件失败: " + configPath, e);
            logger.info("使用默认配置");
        }
    }
    
    /**
     * 保存配置文件
     */
    public void saveConfig() {
        try {
            // 确保配置文件目录存在
            File configFile = new File(configPath);
            File parentDir = configFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);
            
            Files.write(Paths.get(configPath), json.getBytes("UTF-8"));
            logger.info("配置保存成功: " + configPath);
        } catch (IOException e) {
            logger.error("保存配置文件失败: " + configPath, e);
        }
    }
    
    // Getter 和 Setter 方法
    
    public int getMaxLogLines() {
        return maxLogLines;
    }
    
    public void setMaxLogLines(int maxLogLines) {
        this.maxLogLines = maxLogLines;
    }
    
    public boolean isAutoCleanEnabled() {
        return autoCleanEnabled;
    }
    
    public void setAutoCleanEnabled(boolean autoCleanEnabled) {
        this.autoCleanEnabled = autoCleanEnabled;
    }
    
    public int getMaxLogFileCount() {
        return maxLogFileCount;
    }
    
    public void setMaxLogFileCount(int maxLogFileCount) {
        this.maxLogFileCount = maxLogFileCount;
    }
    
    public String getConfigPath() {
        return configPath;
    }
    
    public boolean isSkipMessagesWithFrom() {
        return skipMessagesWithFrom;
    }
    
    public void setSkipMessagesWithFrom(boolean skipMessagesWithFrom) {
        this.skipMessagesWithFrom = skipMessagesWithFrom;
    }
    
    public boolean isSkipMessagesWithTo() {
        return skipMessagesWithTo;
    }
    
    public void setSkipMessagesWithTo(boolean skipMessagesWithTo) {
        this.skipMessagesWithTo = skipMessagesWithTo;
    }
    
    public boolean isSkipMessagesWithBracketPrefix() {
        return skipMessagesWithBracketPrefix;
    }
    
    public void setSkipMessagesWithBracketPrefix(boolean skipMessagesWithBracketPrefix) {
        this.skipMessagesWithBracketPrefix = skipMessagesWithBracketPrefix;
    }
}