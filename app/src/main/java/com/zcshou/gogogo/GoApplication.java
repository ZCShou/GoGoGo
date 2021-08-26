package com.zcshou.gogogo;

import android.app.Application;
import android.os.Environment;

import com.baidu.mapapi.SDKInitializer;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.ConsolePrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoApplication extends Application {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", Locale.CHINA);
    private static final String timeStr = formatter.format(new Date(System.currentTimeMillis()));
    private static final String APP_NAME = "GoGoGo";
    private static final String LOG_FILE_NAME = APP_NAME + "_" + timeStr + ".log";
    private static final long MAX_TIME = 1000 * 60 * 60 * 24 * 5; // 5 days

    @Override
    public void onCreate() {
        super.onCreate();

        // 百度地图 SDK 初始化
        SDKInitializer.initialize(getApplicationContext());
        //
        initXlog();
    }

    /**
     * Initialize XLog.
     */
    private void initXlog() {
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)
                .tag(APP_NAME)                                         // 指定 TAG，默认为 "X-LOG"
                .enableThreadInfo()                                    // 允许打印线程信息，默认禁止
                .enableStackTrace(2)                                   // 允许打印深度为 2 的调用栈信息，默认禁止
                .enableBorder()                                        // 允许打印日志边框，默认禁止
//                .jsonFormatter(new MyJsonFormatter())                  // 指定 JSON 格式化器，默认为 DefaultJsonFormatter
//                .xmlFormatter(new MyXmlFormatter())                    // 指定 XML 格式化器，默认为 DefaultXmlFormatter
//                .throwableFormatter(new MyThrowableFormatter())        // 指定可抛出异常格式化器，默认为 DefaultThrowableFormatter
//                .threadFormatter(new MyThreadFormatter())              // 指定线程信息格式化器，默认为 DefaultThreadFormatter
//                .stackTraceFormatter(new MyStackTraceFormatter())      // 指定调用栈信息格式化器，默认为 DefaultStackTraceFormatter
//                .borderFormatter(new MyBoardFormatter())               // 指定边框格式化器，默认为 DefaultBorderFormatter
//                .addObjectFormatter(AnyClass.class,                    // 为指定类型添加对象格式化器
//                        new AnyClassObjectFormatter())                     // 默认使用 Object.toString()
//                .addInterceptor(new BlacklistTagsFilterInterceptor(    // 添加黑名单 TAG 过滤器
//                        "blacklist1", "blacklist2", "blacklist3"))
//                .addInterceptor(new MyInterceptor())                   // 添加一个日志拦截器
                .build();

        Printer androidPrinter = new AndroidPrinter(true);  // 通过 android.util.Log 打印日志的打印器
        Printer consolePrinter = new ConsolePrinter();                  // 通过 System.out 打印日志到控制台的打印器
        Printer filePrinter = new FilePrinter                           // 打印日志到文件的打印器
                .Builder(new File(Environment.getExternalStorageDirectory(),
                APP_NAME).getPath())                             // 指定保存日志文件的路径
                .fileNameGenerator(new ChangelessFileNameGenerator(LOG_FILE_NAME))         // 指定日志文件名生成器，默认为 ChangelessFileNameGenerator("log")
                .backupStrategy(new NeverBackupStrategy())              // 指定日志文件备份策略，默认为 FileSizeBackupStrategy(1024 * 1024)
                .cleanStrategy(new FileLastModifiedCleanStrategy(MAX_TIME))     // 指定日志文件清除策略，默认为 NeverCleanStrategy()
                .build();

        XLog.init(                                                 // 初始化 XLog
                config,                                                // 指定日志配置，如果不指定，会默认使用 new LogConfiguration.Builder().build()
                androidPrinter,                                        // 添加任意多的打印器。如果没有添加任何打印器，会默认使用 AndroidPrinter(Android)/ConsolePrinter(java)
                consolePrinter,
                filePrinter);
    }
}