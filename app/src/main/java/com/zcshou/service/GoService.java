package com.zcshou.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.MainActivity;
import com.zcshou.joystick.JoyStick;
import com.zcshou.log4j.LogUtil;
import com.zcshou.gogogo.R;

import org.apache.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoService extends Service {

    public static final int RunCode = 0x01;
    public static final int StopCode = 0x02;
    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;
    private boolean isStop = true;  // 是否启动了模拟位置
    private String curLatLng = "117.027707&36.667662";// 模拟位置的经纬度字符串
    private static final long mTS = 1609286402;

    // 摇杆相关
    private JoyStick mJoyStick;
    private boolean isJoyStick = false; // 摇杆是否启动
    double mSpeed;
    private boolean isLimit = false;
    private TimeTask timeTask;
    private ExecutorService threadExecutor;
    private TimeCount time;

    NoteActionReceiver acReceiver;

    // log debug
    private static final Logger log = Logger.getLogger(GoService.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        LogUtil.configLog();

        Log.d("GoService", "onCreate");
        log.debug("onCreate");

        super.onCreate();

        time = new TimeCount(1000 * 60 * 20, 1000);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //remove default network location provider
        rmNetworkTestProvider();
        //remove gps provider
        rmGPSTestProvider();
        //remove other provider
        //rmOtherTestProvider();

        //add a new test network location provider
        setNetworkTestProvider();
        // add a new GPS test Provider
        setGPSTestProvider();

        //thread
        handlerThread = new HandlerThread(getUUID(), -2);
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(80);

                    if (!isStop) {
                        setNetworkLocation();
                        setGPSLocation();

                        sendEmptyMessage(0);

                        // broadcast to MainActivity
                        Intent intent = new Intent();
                        intent.putExtra("StatusRun", RunCode);
                        // intent.putExtra("CurLatLng", curLatLng);
                        intent.setAction("com.zcshou.service.GoService");
                        sendBroadcast(intent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d("GoService", "handleMessage error");
                    log.debug("handleMessage error");
                    Thread.currentThread().interrupt();
                }
            }
        };
        handler.sendEmptyMessage(0);

        mSpeed = 0.00003;

        timeTask = new TimeTask();
        threadExecutor = Executors.newSingleThreadExecutor();

        acReceiver = new NoteActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("ShowJoyStick");
        filter.addAction("HideJoyStick");
        registerReceiver(acReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GoService", "onStartCommand");
        log.debug("onStartCommand");

        threadExecutor.submit(timeTask);
        time.start();

        String channelId = "channel_01";
        String name = "channel_name";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;
        //准备intent
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent showIntent = new Intent("ShowJoyStick");
        PendingIntent showPendingPI = PendingIntent.getBroadcast(this, 0, showIntent, PendingIntent.FLAG_CANCEL_CURRENT );
        Intent hideIntent = new Intent("HideJoyStick");
        PendingIntent hidePendingPI = PendingIntent.getBroadcast(this, 0, hideIntent, PendingIntent.FLAG_CANCEL_CURRENT );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
            Log.i("GoService", mChannel.toString());

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }

            notification = new NotificationCompat.Builder(this, channelId)
                    .setChannelId(channelId)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.app_service))
                    .setContentIntent(clickPI)
                    .addAction(new NotificationCompat.Action(null, "显示摇杆", showPendingPI))
                    .addAction(new NotificationCompat.Action(null, "隐藏摇杆", hidePendingPI))
                    .setSmallIcon(R.mipmap.ic_launcher).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID")
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText(getResources().getString(R.string.app_service))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(clickPI)
                    .addAction(new NotificationCompat.Action(null, "显示摇杆", showPendingPI))
                    .addAction(new NotificationCompat.Action(null, "隐藏摇杆", hidePendingPI))
                    .setOngoing(true)
                    .setChannelId(channelId);//无效
            notification = notificationBuilder.build();
        }

        startForeground(1, notification);

        // get location info from mainActivity
        curLatLng = intent.getStringExtra("CurLatLng");

        Log.d("GoService", "LatLng from Main is " + curLatLng);
        log.debug("LatLng from Main is " + curLatLng);

        //start to refresh location
        isStop = false;

        // 开启摇杆
        if (!isJoyStick) {
            mJoyStick = new JoyStick(this);
            mJoyStick.setListener(new JoyStick.JoyStickClickListener() {
                @Override
                public void clickAngleInfo(double angle, double speed) {
                    mSpeed = speed * 3.6;   // 转换为 km/h, 1米/秒(m/s)=3.6千米/时(km/h)
                    if (!isLimit) {
                        // 注意：这里的 x y 与 圆中角度的对应问题（以 X 轴正向为 0 度）
                        double x = Math.cos(angle * 2 * Math.PI / 360);   // 注意安卓使用的是弧度
                        double y = Math.sin(angle * 2 * Math.PI / 360);   // 注意安卓使用的是弧度

                        // 根据当前的经纬度和距离，计算下一个经纬度
                        // Latitude: 1 deg = 110.574 km // 纬度的每度的距离大约为 110.574km
                        // Longitude: 1 deg = 111.320*cos(latitude) km  // 经度的每度的距离从0km到111km不等
                        // 具体见：http://wp.mlab.tw/?p=2200

                        String[] latLngStr = curLatLng.split("&");

                        double lngDegree = mSpeed * x / (111.320 * Math.cos(Math.abs(Double.parseDouble(latLngStr[1])) * Math.PI / 180));
                        double latDegree = mSpeed * y / 110.574;

                        double lng = Double.parseDouble(latLngStr[0]) + lngDegree / 1000;   // 为啥 / 1000 ? 按照速度算下来，这里偏大
                        double lat = Double.parseDouble(latLngStr[1]) + latDegree / 1000;   // 为啥 / 1000 ? 按照速度算下来，这里偏大
                        curLatLng = lng + "&" + lat;
                    } else {
                        Log.d("GoService", "isLimit " + isLimit);
                        log.debug("isLimit " + isLimit);
                    }
                }

                @Override
                public void setCurrentSpeed(double speed) {
                    mSpeed = speed * 3.6;   // 转换为 km/h, 1米/秒(m/s)=3.6千米/时(km/h)
                }

            });

            mJoyStick.show();

            isJoyStick = true;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("GoService", "onDestroy");
        log.debug("onDestroy");

        isStop = true;

        mJoyStick.hide();
        isJoyStick = false;

        handler.removeMessages(0);
        handlerThread.quit();
        time.cancel();
        threadExecutor.shutdownNow();

        unregisterReceiver(acReceiver);

        //remove test provider
        rmNetworkTestProvider();
        rmGPSTestProvider();
        //rmOtherTestProvider();

        stopForeground(true);

        // broadcast to MainActivity
        Intent intent = new Intent();
        intent.putExtra("StatusRun", StopCode);
        intent.setAction("com.zcshou.service.GoService");
        sendBroadcast(intent);

        super.onDestroy();
    }

    //generate a location
    public Location generateLocation(LatLng latLng) {
        Location loc = new Location("gps");
        loc.setAccuracy(2.0F);                  // 精度（米）
        loc.setAltitude(55.0D);                 // 高度（米）
        loc.setBearing(1.0F);                   // 方向（度）
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 7);
        loc.setExtras(bundle);
        loc.setLatitude(latLng.latitude);       // 纬度（度）
        loc.setLongitude(latLng.longitude);     // 经度（度）
        loc.setTime(System.currentTimeMillis());    // 本地时间

        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        return loc;
    }

    //添加网络定位
    private void setNetworkLocation() {
        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));

        String providerStr = LocationManager.NETWORK_PROVIDER;

        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
        } catch (Exception e) {
            Log.d("GoService", "setNetworkLocation error");
            log.debug("setNetworkLocation error");
            e.printStackTrace();
        }
    }

    //set gps location
    private void setGPSLocation() {
        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));
        String providerStr = LocationManager.GPS_PROVIDER;

        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
        } catch (Exception e) {
            Log.d("GoService", "setGPSLocation error");
            log.debug("setGPSLocation error");
            e.printStackTrace();
        }
    }

    //remove network provider
    private void rmNetworkTestProvider() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d("GoService", "now remove NetworkProvider");
                log.debug("now remove NetworkProvider");
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            } else {
                Log.d("GoService", "NetworkProvider is not enabled");
                log.debug("NetworkProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("GoService", "rmNetworkProvider error");
            log.debug("rmNetworkProvider error");
        }
    }

    //set network provider
    private void setNetworkTestProvider() {
        try {
            locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                    false, false, false, false,
                    false, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            Log.d("GoService", "addTestProvider[NETWORK_PROVIDER] success");
            log.debug("addTestProvider[NETWORK_PROVIDER] success");
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d("GoService", "addTestProvider[NETWORK_PROVIDER] error");
            log.debug("addTestProvider[NETWORK_PROVIDER] error");
        }

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("GoService", "setTestProviderEnabled[NETWORK_PROVIDER] error");
                log.debug("setTestProviderEnabled[NETWORK_PROVIDER] error");
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // 根据 google 的文档，API 29 此方法无效。
            locationManager.setTestProviderStatus(LocationManager.NETWORK_PROVIDER, LocationProvider.AVAILABLE, null,
                    System.currentTimeMillis());
        }
    }

    // set GPS provider
    private void rmGPSTestProvider() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d("GoService", "now remove GPSProvider");
                log.debug("now remove GPSProvider");
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } else {
                Log.d("GoService", "GPSProvider is not enabled");
                log.debug("GPSProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("GoService", "rmGPSProvider error");
            log.debug("rmGPSProvider error");
        }
    }

    private void setGPSTestProvider() {
//        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
//            locationManager.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, false);
//            Log.d("GoService", "Disable passive provider");
//            log.debug("Disable passive provider");
//        }
//
//        if (!locationManager.isProviderEnabled("fused") && Build.VERSION.SDK_INT >= 29
//                && !RomUtils.isVivo() && !RomUtils.isEmui()) {    // 目前 ViVo 会崩溃
//            locationManager.setTestProviderEnabled("fused", false);
//            Log.d("GoService", "Disable fused provider");
//            log.debug("Disable fused provider");
//        }

        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, true,
                    false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_HIGH);
            Log.d("GoService", "addTestProvider[GPS_PROVIDER] success");
            log.debug("addTestProvider[GPS_PROVIDER] success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("GoService", "addTestProvider[GPS_PROVIDER] error");
            log.debug("addTestProvider[GPS_PROVIDER] error");
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("GoService", "setTestProviderEnabled[GPS_PROVIDER] error");
                log.debug("setTestProviderEnabled[GPS_PROVIDER] error");
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // 根据 google 的文档，API 29 此方法无效。
            locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null,
                    System.currentTimeMillis());
        }
    }

//    // set other provider
//    private void rmOtherTestProvider() {
//        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
//            try {
//                locationManager.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, false);
//                Log.d("GoService", "Disable passive provider");
//                log.debug("Disable passive provider");
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.d("GoService", "Disable passive provider error");
//                log.debug("Disable passive provider error");
//            }
//        }
//
//        if (!locationManager.isProviderEnabled("fused") && Build.VERSION.SDK_INT >= 29 && !RomUtils.isVivo() && !RomUtils.isEmui()) {   // 目前 ViVo 会崩溃
//            try {
//                locationManager.setTestProviderEnabled("fused", false);
//                Log.d("GoService", "Disable fused provider");
//                log.debug("Disable fused provider");
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.d("GoService", "Disable fused provider error");
//                log.debug("Disable fused provider error");
//            }
//        }
//    }

    //uuid random
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            threadExecutor.submit(timeTask);
            time.start();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示

        }
    }

    private class TimeTask implements Runnable {
        private final String[] ntpServerPool = {"ntp1.aliyun.com", "ntp2.aliyun.com", "ntp3.aliyun.com", "ntp4.aliyun.com", "ntp5.aliyun.com", "ntp6.aliyun.com", "ntp7.aliyun.com",
                "cn.pool.ntp.org", "cn.ntp.org.cn", "sg.pool.ntp.org", "tw.pool.ntp.org", "jp.pool.ntp.org", "hk.pool.ntp.org", "th.pool.ntp.org",
                "time.windows.com", "time.nist.gov", "time.apple.com", "time.asia.apple.com",
                "dns1.synet.edu.cn", "news.neu.edu.cn", "dns.sjtu.edu.cn", "dns2.synet.edu.cn", "ntp.glnet.edu.cn", "s2g.time.edu.cn",
                "ntp-sz.chl.la", "ntp.gwadar.cn", "3.asia.pool.ntp.org"};

        @Override
        public void run() {
            GoSntpClient GoSntpClient = new GoSntpClient();
            int i;
            for (i = 0; i < ntpServerPool.length; i++) {
                if (GoSntpClient.requestTime(ntpServerPool[i], 30000)) {
                    long now = GoSntpClient.getNtpTime() + SystemClock.elapsedRealtime() - GoSntpClient.getNtpTimeReference();
                    if (now / 1000 > mTS) {
                        isLimit = true;
                    }
                    break;
                }
            }

            if (i >= ntpServerPool.length) {
                isLimit = true;
                Log.d("GoService", "GoSntpClient is error");
                log.debug("GoSntpClient is error");
            }
        }
    }

    public class NoteActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("ShowJoyStick")) {
                    Log.d("GoService", "ShowJoyStick");
                    log.debug("ShowJoyStick");
                    mJoyStick.show();
                }

                if (action.equals("HideJoyStick")) {
                    mJoyStick.hide();
                    Log.d("GoService", "HideJoyStick");
                    log.debug("HideJoyStick");
                }
            }
        }
    }

}


