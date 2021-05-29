package com.zcshou.log4j;

import java.io.IOException;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.LogLog;


/**
 * 源自 android-logging-log4j-1.0.3.jar
 *
 * @author Administrator
 */
public class LogConfig {
    private Level rootLevel = Level.DEBUG;
    /**
     *  ### log文件的格式
     *
     * 	### 输出格式解释：
     *	### [%-d{yyyy-MM-dd HH:mm:ss}][Class: %c.%M(%F:%L)] %n[Level: %-5p] - Msg: %m%n
     *
     *	### %d{yyyy-MM-dd HH:mm:ss}: 时间，大括号内是时间格式
     *	### %c: 全类名
     *	### %M: 调用的方法名称
     *	### %F:%L  类名:行号（在控制台可以追踪代码）
     *	###	%n: 换行
     *	### %p: 日志级别，这里%-5p是指定的5个字符的日志名称，为的是格式整齐
     *	### %m: 日志信息
    
     *	### 输出的信息大概如下：
     *	### [时间{时间格式}][信息所在的class.method(className：lineNumber)] 换行
     *	###	[Level: 5个字符的等级名称] - Msg: 输出信息 换行
     */
    private String filePattern = "[%-d{yyyy-MM-dd HH:mm:ss}][Class: %c.%M(%F:%L)] %n[Level: %-5p] - Msg: %m%n";
    
    /**
     * ### LogCat控制台输出格式
     *
     * ### [Class: 信息所在的class.method(className：lineNumber)] 换行
     * ### [Level: 5个字符的等级名称] - Msg: 输出信息 换行
     */
    private String logCatPattern = "[Class: %c.%M(%F:%L)] %n[Level: %-5p] - Msg: %m%n";
    private String fileName = "android-log4j.log";
    private int maxBackupSize = 5;
    private long maxFileSize = 1024 * 1024 * 5L;
    private boolean immediateFlush = true;
    private boolean useLogCatAppender = true;
    private boolean useFileAppender = true;
    private boolean resetConfiguration = true;
    private boolean internalDebugging = false;
    
    public LogConfig() {
    }
    
    public LogConfig(String fileName) {
        setFileName(fileName);
    }
    
    public LogConfig(String fileName, Level rootLevel) {
        this(fileName);
        setRootLevel(rootLevel);
    }
    
    public LogConfig(String fileName, Level rootLevel, String filePattern) {
        this(fileName);
        setRootLevel(rootLevel);
        setFilePattern(filePattern);
    }
    
    public LogConfig(String fileName, int maxBackupSize, long maxFileSize, String filePattern, Level rootLevel) {
        this(fileName, rootLevel, filePattern);
        setMaxBackupSize(maxBackupSize);
        setMaxFileSize(maxFileSize);
    }
    
    public void configure() {
        Logger root = Logger.getRootLogger();
        
        if (isResetConfiguration()) {
            LogManager.getLoggerRepository().resetConfiguration();
        }
        
        LogLog.setInternalDebugging(isInternalDebugging());
        
        if (isUseFileAppender()) {
            configureFileAppender();
        }
        
        if (isUseLogCatAppender()) {
            configureLogCatAppender();
        }
        
        root.setLevel(getRootLevel());
    }
    
    public void setLevel(String loggerName, Level level) {
        Logger.getLogger(loggerName).setLevel(level);
    }
    
    private void configureFileAppender() {
        Logger root = Logger.getRootLogger();
        Layout fileLayout = new PatternLayout(getFilePattern());
        RollingFileAppender rollingFileAppender;
        
        try {
            //here
            rollingFileAppender = new RollingFileAppender(fileLayout, getFileName());
        } catch (IOException e) {
            throw new RuntimeException("Exception configuring log system", e);
        }
        
        rollingFileAppender.setMaxBackupIndex(getMaxBackupSize());
        rollingFileAppender.setMaximumFileSize(getMaxFileSize());
        rollingFileAppender.setImmediateFlush(isImmediateFlush());
        root.addAppender(rollingFileAppender);
    }
    
    private void configureLogCatAppender() {
        Logger root = Logger.getRootLogger();
        Layout logCatLayout = new PatternLayout(getLogCatPattern());
        LogCatAppender logCatAppender = new LogCatAppender(logCatLayout);
        root.addAppender(logCatAppender);
    }
    
    public Level getRootLevel() {
        return this.rootLevel;
    }
    
    public void setRootLevel(Level level) {
        this.rootLevel = level;
    }
    
    public String getFilePattern() {
        return this.filePattern;
    }
    
    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }
    
    public String getLogCatPattern() {
        return this.logCatPattern;
    }
    
    public void setLogCatPattern(String logCatPattern) {
        this.logCatPattern = logCatPattern;
    }
    
    public String getFileName() {
        return this.fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public int getMaxBackupSize() {
        return this.maxBackupSize;
    }
    
    public void setMaxBackupSize(int maxBackupSize) {
        this.maxBackupSize = maxBackupSize;
    }
    
    public long getMaxFileSize() {
        return this.maxFileSize;
    }
    
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public boolean isImmediateFlush() {
        return this.immediateFlush;
    }
    
    public void setImmediateFlush(boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
    
    public boolean isUseFileAppender() {
        return this.useFileAppender;
    }
    
    public void setUseFileAppender(boolean useFileAppender) {
        this.useFileAppender = useFileAppender;
    }
    
    public boolean isUseLogCatAppender() {
        return this.useLogCatAppender;
    }
    
    public void setUseLogCatAppender(boolean useLogCatAppender) {
        this.useLogCatAppender = useLogCatAppender;
    }
    
    public void setResetConfiguration(boolean resetConfiguration) {
        this.resetConfiguration = resetConfiguration;
    }
    
    public boolean isResetConfiguration() {
        return this.resetConfiguration;
    }
    
    public void setInternalDebugging(boolean internalDebugging) {
        this.internalDebugging = internalDebugging;
    }
    
    public boolean isInternalDebugging() {
        return this.internalDebugging;
    }
}