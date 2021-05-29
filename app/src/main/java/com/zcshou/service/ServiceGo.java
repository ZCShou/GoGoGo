package com.zcshou.service;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.baidu.mapapi.model.LatLng;
import com.zcshou.gogogo.MainActivity;
import com.zcshou.joystick.JoyStick;
import com.zcshou.gogogo.R;

//import android.annotation.NonNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceGo extends Service {
    // 对外变量
    public static final int SERVICE_GO_RUNNING = 0x01;
    public static final int SERVICE_GO_STOPPED = 0x02;
    public static final String SERVICE_STATUS = "ServiceStatus";
    public static final String SERVICE_NAME = "ServiceGo";
    // 内部变量
    private static final int HANDLER_MSG_ID = 0;
    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isStop = true;  // 是否启动了模拟位置
    private String curLatLng = "117.027707&36.667662";// 模拟位置的经纬度字符串

    // 摇杆相关
    private JoyStick mJoyStick;
    double mSpeed;
    // 限制检测
    private boolean isLimit = false;
    private static final long mTS = 1636588802;
    private TimeTask timeTask;
    private ExecutorService threadExecutor;
    // 通知栏消息
    NoteActionReceiver acReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSpeed = 0.00003;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        removeTestProviderNetwork();
        removeTestProviderGPS();

        addTestProviderNetwork();
        addTestProviderGPS();

        // 创建 HandlerThread 实例，第一个参数是线程的名字
        handlerThread = new HandlerThread(getUUID(), Process.THREAD_PRIORITY_FOREGROUND);
        // 启动 HandlerThread 线程
        handlerThread.start();
        // Handler 对象与 HandlerThread 的 Looper 对象的绑定
        handler = new Handler(handlerThread.getLooper()) {
            // 这里的Handler对象可以看作是绑定在HandlerThread子线程中，所以handlerMessage里的操作是在子线程中运行的
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(80);

                    if (!isStop) {
                        setNetworkLocation();
                        setGPSLocation();

                        sendEmptyMessage(HANDLER_MSG_ID);

                        // broadcast to MainActivity
                        Intent intent = new Intent();
                        intent.putExtra(SERVICE_STATUS, SERVICE_GO_RUNNING);
                        intent.setAction(SERVICE_NAME);
                        sendBroadcast(intent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d("ServiceGo", "handleMessage error");
                    Thread.currentThread().interrupt();
                }
            }
        };
        handler.sendEmptyMessage(HANDLER_MSG_ID);

        timeTask = new TimeTask();
        threadExecutor = Executors.newSingleThreadExecutor();

        acReceiver = new NoteActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("ShowJoyStick");
        filter.addAction("HideJoyStick");
        registerReceiver(acReceiver, filter);

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
                    Log.d("ServiceGo", "isLimit ");
                }
            }

            @Override
            public void setCurrentSpeed(double speed) {
                mSpeed = speed * 3.6;   // 转换为 km/h, 1米/秒(m/s)=3.6千米/时(km/h)
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ServiceGo", "onStartCommand");

        threadExecutor.submit(timeTask);

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

        NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
        Log.i("ServiceGo", mChannel.toString());

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

        startForeground(1, notification);

        curLatLng = intent.getStringExtra("CurLatLng");
        Log.d("ServiceGo", "LatLng from Main is " + curLatLng);

        isStop = false;
        mJoyStick.show();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("ServiceGo", "onDestroy");
        isStop = true;

        handler.removeMessages(HANDLER_MSG_ID);
        handlerThread.quit();
        threadExecutor.shutdownNow();

        unregisterReceiver(acReceiver);

        removeTestProviderNetwork();
        removeTestProviderGPS();
        //rmOtherTestProvider();

        stopForeground(true);

        // broadcast to MainActivity
        Intent intent = new Intent();
        intent.putExtra(SERVICE_STATUS, SERVICE_GO_STOPPED);
        intent.setAction(SERVICE_NAME);
        sendBroadcast(intent);

        super.onDestroy();
    }

    // 生成一个位置
    public Location makeLocation(String provider, LatLng latLng) {
        Location loc = new Location(provider);  // 参数是提供位置的源
        loc.setAccuracy(Criteria.ACCURACY_FINE);                  // 设定此位置的估计水平精度，以米为单位。
        loc.setAltitude(55.0D);                 // 设置高度，在 WGS 84 参考坐标系中的米
        loc.setBearing(1.0F);                   // 方向（度）
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 7);
        loc.setExtras(bundle);
        loc.setLatitude(latLng.latitude);       // 纬度（度）
        loc.setLongitude(latLng.longitude);     // 经度（度）
        loc.setTime(System.currentTimeMillis());    // 本地时间
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            loc.setMock(false);
//        }
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        return loc;
    }

    private void setNetworkLocation() {
        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));

        try {
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, makeLocation(LocationManager.NETWORK_PROVIDER, latLng));
        } catch (Exception e) {
            Log.d("ServiceGo", "setNetworkLocation error");
            e.printStackTrace();
        }
    }

    private void setGPSLocation() {
        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));

        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, makeLocation(LocationManager.NETWORK_PROVIDER, latLng));
        } catch (Exception e) {
            Log.d("ServiceGo", "setGPSLocation error");
            e.printStackTrace();
        }
    }

    private void removeTestProviderNetwork() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            } else {
                Log.d("ServiceGo", "NetworkProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ServiceGo", "ERROR:removeTestProviderNetwork");
        }
    }

    private void addTestProviderNetwork() {
        try {
            locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                    false, false, true, false,
                    true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d("ServiceGo", "ERROR:addTestProviderNetwork");
        }

    }

    private void removeTestProviderGPS() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } else {
                Log.d("ServiceGo", "GPSProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ServiceGo", "ERROR:removeTestProviderGPS");
        }
    }

    private void addTestProviderGPS() {
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, true,
                    false, true, false, true, Criteria.POWER_HIGH, Criteria.ACCURACY_MEDIUM);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("ServiceGo", "ERROR:addTestProviderGPS");
        }
    }

//    // set other provider
//    private void rmOtherTestProvider() {
//        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
//            try {
//                locationManager.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, false);
//                Log.d("ServiceGo", "Disable passive provider");
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.d("ServiceGo", "Disable passive provider error");
//            }
//        }
//
//        if (!locationManager.isProviderEnabled("fused") && Build.VERSION.SDK_INT >= 29 && !RomUtils.isVivo() && !RomUtils.isEmui()) {   // 目前 ViVo 会崩溃
//            try {
//                locationManager.setTestProviderEnabled("fused", false);
//                Log.d("ServiceGo", "Disable fused provider");
//            } catch(Exception e) {
//                e.printStackTrace();
//                Log.d("ServiceGo", "Disable fused provider error");
//            }
//        }
//    }

    // uuid random
    public static String getUUID() {
        return UUID.randomUUID().toString();
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
                Log.d("ServiceGo", "GoSntpClient is error");
            }
        }
    }

    public class NoteActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("ShowJoyStick")) {
                    Log.d("ServiceGo", "ShowJoyStick");
                    mJoyStick.show();
                }

                if (action.equals("HideJoyStick")) {
                    mJoyStick.hide();
                    Log.d("ServiceGo", "HideJoyStick");
                }
            }
        }
    }
}


