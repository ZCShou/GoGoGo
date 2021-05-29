package com.zcshou.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import android.util.Log;

/**
 * 源自 android-logging-log4j-1.0.3.jar
 *
 * @author Administrator
 */
public class LogCatAppender extends AppenderSkeleton {
    protected Layout tagLayout;
    
    public LogCatAppender(Layout messageLayout, Layout tagLayout) {
        this.tagLayout = tagLayout;
        setLayout(messageLayout);
    }
    
    public LogCatAppender(Layout messageLayout) {
        //这里定义的是Tag名称
        this(messageLayout, new PatternLayout("%c"));
    }
    
    public LogCatAppender() {
        this(new PatternLayout("%c"));
    }
    
    protected void append(LoggingEvent le) {
        switch (le.getLevel().toInt()) {
        case 5000:
            if (le.getThrowableInformation() != null) {
                Log.v(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else {
                Log.v(getTagLayout().format(le), getLayout().format(le));
            }
            
            break;
            
        case 10000:
            if (le.getThrowableInformation() != null) {
                Log.d(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else {
                Log.d(getTagLayout().format(le), getLayout().format(le));
            }
            
            break;
            
        case 20000:
            if (le.getThrowableInformation() != null) {
                Log.i(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else {
                Log.i(getTagLayout().format(le), getLayout().format(le));
            }
            
            break;
            
        case 30000:
            if (le.getThrowableInformation() != null) {
                Log.w(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else {
                Log.w(getTagLayout().format(le), getLayout().format(le));
            }
            
            break;
            
        case 40000:
            if (le.getThrowableInformation() != null) {
                Log.e(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else {
                Log.e(getTagLayout().format(le), getLayout().format(le));
            }
            
            break;
            
        case 50000:
            if (le.getThrowableInformation() != null) {
                Log.wtf(getTagLayout().format(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
            } else
                Log.wtf(getTagLayout().format(le), getLayout().format(le));
                
            break;
        }
    }
    
    public void close() {
    }
    
    public boolean requiresLayout() {
        return true;
    }
    
    public Layout getTagLayout() {
        return this.tagLayout;
    }
    
    public void setTagLayout(Layout tagLayout) {
        this.tagLayout = tagLayout;
    }
}
