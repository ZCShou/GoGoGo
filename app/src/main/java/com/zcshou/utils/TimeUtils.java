package com.zcshou.utils;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {
    /**
     * 获取当前时间
     *
     * @return string
     */
    public static String getNowTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    /**
     * 获取时间戳
     *
     * @return 获取时间戳
     */
    public static String getTimeString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        return df.format(calendar.getTime());
    }

    /**
     * 时间转换为时间戳
     *
     * @param time:需要转换的时间
     * @return string
     */
    public static String dateToStamp(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = null;
        try {
            date = simpleDateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (date == null) {
            return null;
        } else {
            long ts = date.getTime();
            return String.valueOf(ts);
        }
    }

    /**
     * 时间戳转换为字符串
     *
     * @param time:时间戳
     * @return string
     */
    public static String times(String time) {
        SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH时mm分", Locale.getDefault());
        @SuppressWarnings("unused")
        long lcc = Long.parseLong(time);
        int i = Integer.parseInt(time);
        return sdr.format(new Date(i * 1000L));

    }

    /**
     * 获取距现在某一小时的时刻
     *
     * @param hour hour=-1为上一个小时，hour=1为下一个小时
     * @return string
     */
    public static String getLongTime(int hour) {
        Calendar c = Calendar.getInstance(); // 当时的日期和时间      
        int h; // 需要更改的小时      
        h = c.get(Calendar.HOUR_OF_DAY) - hour;
        c.set(Calendar.HOUR_OF_DAY, h);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.v("time", df.format(c.getTime()));
        return df.format(c.getTime());
    }

    /**
     * 比较时间大小
     *
     * @param str1：要比较的时间
     * @param str2：要比较的时间
     * @return string
     */
    public static boolean isDateOneBigger(String str1, String str2) {
        boolean isBigger = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date dt1 = null;
        Date dt2 = null;
        try {
            dt1 = sdf.parse(str1);
            dt2 = sdf.parse(str2);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (dt1 == null || dt2 == null) {
            return false;
        } else {
            if (dt1.getTime() > dt2.getTime()) {
                isBigger = true;
            }
            return isBigger;
        }
    }

    /**
     * 当地时间 ---> UTC时间
     *
     * @return string
     */
    public static String Local2UTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        return sdf.format(new Date());
    }

    /**
     * UTC时间 ---> 当地时间
     *
     * @param utcTime UTC时间
     * @return s
     */
    public static String utc2Local(String utcTime) {
        SimpleDateFormat utcFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());//UTC时间格式
        utcFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date gpsUTCDate = null;
        try {
            gpsUTCDate = utcFormater.parse(utcTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat localFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());//当地时间格式
        localFormater.setTimeZone(TimeZone.getDefault());
        if (gpsUTCDate == null) {
            return null;
        } else {
            return localFormater.format(gpsUTCDate.getTime());
        }
    }
}