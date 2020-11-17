package com.zcshou.log4j;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

import org.apache.log4j.Level;

import android.os.Environment;

/**
 * LogUtil 工具类
 *
 * @author Administrator
 */
@SuppressWarnings("all")
public class LogUtil {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd");
    private static final String timeStr = formatter.format(new Date(System.currentTimeMillis()));
    /**
     * 这里的AppName决定log的文件位置和名称
     **/
    private static final String APP_NAME = "GoGoGo";
    
    /**
     * 设置log文件全路径，这里是 MyApp/Log/myapp.log
     **/
    private static final String LOG_FILE_PATH = Environment.getExternalStorageDirectory() + File.separator + APP_NAME
            + File.separator + "Log" + File.separator + APP_NAME.toLowerCase(Locale.CHINA) + timeStr + ".log";
            
    /**
     * ### log文件的格式
     * <p>
     * ### 输出格式解释：
     * ### [%-d{yyyy-MM-dd HH:mm:ss}][Class: %c.%M(%F:%L)] %n[Level: %-5p] - Msg: %m%n
     * <p>
     * ### %d{yyyy-MM-dd HH:mm:ss}: 时间，大括号内是时间格式
     * ### %c: 全类名
     * ### %M: 调用的方法名称
     * ### %F:%L  类名:行号（在控制台可以追踪代码）
     * ###	%n: 换行
     * ### %p: 日志级别，这里%-5p是指定的5个字符的日志名称，为的是格式整齐
     * ### %m: 日志信息
     * <p>
     * ### 输出的信息大概如下：
     * ### [时间{时间格式}][信息所在的class.method(className：lineNumber)] 换行
     * ###	[Level: 5个字符的等级名称] - Msg: 输出信息 换行
     */
    private static final String LOG_FILE_PATTERN = "[%-d{yyyy-MM-dd HH:mm:ss}][Class: %c.%M(%F:%L)] %n[Level: %-5p] - Msg: %m%n";
    
    /**
     * 生产环境下的log等级
     **/
    private static final Level LOG_LEVEL_PRODUCE = Level.ALL;
    
    /**
     * 发布以后的log等级
     **/
    private static final Level LOG_LEVEL_RELEASE = Level.INFO;
    
    /**
     * 配置log4j参数
     */
    public static void configLog() throws IOException {
        LogConfig logConfig = new LogConfig();
        /** 设置Log等级，生产环境下调用setLogToProduce()，发布后调用setLogToRelease() **/
        setLogToProduce(logConfig);
        //        setLogToRelease(logConfig);
        logConfig.setFileName(LOG_FILE_PATH);
        logConfig.setLevel("org.apache", Level.ERROR);
        logConfig.setFilePattern(LOG_FILE_PATTERN);
        logConfig.setMaxFileSize(1024 * 1024 * 5);
        logConfig.setImmediateFlush(true);
        logConfig.configure();
    }

    /**
     * 获取日志文件
     *
     * @param logConfig
     */
    public static String getLogFile() {
        return LOG_FILE_PATH;
    }
    
    /**
     * 将log设置为生产环境
     *
     * @param logConfig
     */
    private static void setLogToProduce(LogConfig logConfig) {
        logConfig.setRootLevel(LOG_LEVEL_PRODUCE);
    }
    
    /**
     * 将log设置为发布以后的环境
     *
     * @param logConfig
     */
    private static void setLogToRelease(LogConfig logConfig) {
        logConfig.setRootLevel(LOG_LEVEL_RELEASE);
    }
}
