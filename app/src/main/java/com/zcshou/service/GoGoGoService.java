package com.zcshou.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;
import com.zcshou.joystick.JoyStick;
import com.zcshou.log4j.LogUtil;
import com.zcshou.gogogo.R;

import org.apache.log4j.Logger;

import java.util.UUID;

public class GoGoGoService extends Service {

    public static final int RunCode = 0x01;
    public static final int StopCode = 0x02;

    private final String TAG = "GoGoGoService";
    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;
    private boolean isStop = true;
    //经纬度字符串
    private String curLatLng = "104.06121778639009&30.544111926165282";

    // 摇杆
    private JoyStick mJoyStick;
    // 摇杆是否启动
    private boolean isJoyStick = false;
    double mSpeed;

    //log debug
    private static final Logger log = Logger.getLogger(GoGoGoService.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        LogUtil.configLog();

        Log.d(TAG, "onCreate");
        log.debug(TAG + ": onCreate");

        super.onCreate();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //remove default network location provider
        rmNetworkTestProvider();
        //remove gps provider
        rmGPSTestProvider();
        //remove other provider
        rmOtherTestProvider();

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
                        intent.setAction("com.zcshou.service.GoGoGoService");
                        sendBroadcast(intent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "handleMessage error");
                    log.debug(TAG + ": handleMessage error");
                    Thread.currentThread().interrupt();
                }
            }
        };
        handler.sendEmptyMessage(0);

        mSpeed = 0.00003;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        log.debug(TAG + ": onStartCommand");
        String channelId = "channel_01";
        String name = "channel_name";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
            Log.i(TAG, mChannel.toString());

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }

            notification = new NotificationCompat.Builder(this, channelId)
                    .setChannelId(channelId)
                    .setContentTitle("GoGoGo")
                    .setContentText("GoGoGo service is running")
                    .setSmallIcon(R.mipmap.ic_launcher).build();
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID")
                    .setContentTitle("GoGoGo")
                    .setContentText("GoGoGo service is running")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setChannelId(channelId);//无效
            notification = notificationBuilder.build();
        }

        startForeground(1, notification);

        // get location info from mainActivity
        curLatLng = intent.getStringExtra("CurLatLng");
        Log.d(TAG, "DataFromMain is " + curLatLng);
        log.debug(TAG + ": DataFromMain is " + curLatLng);
        //start to refresh location
        isStop = false;

        // 开启摇杆
        if (!isJoyStick) {
            mJoyStick = new JoyStick(this);
            mJoyStick.setListener(new JoyStick.JoyStickClickListener() {
                @Override
                public void clickAngleInfo(double angle, double speed) {
                    mSpeed = speed * 3.6;   // 转换为 km/h, 1米/秒(m/s)=3.6千米/时(km/h)
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
        Log.d(TAG, "onDestroy");
        log.debug(TAG + ": onDestroy");
        isStop = true;

        mJoyStick.hide();
        isJoyStick = false;

        handler.removeMessages(0);
        handlerThread.quit();

        //remove test provider
        rmNetworkTestProvider();
        rmGPSTestProvider();
        rmOtherTestProvider();

        stopForeground(true);

        // broadcast to MainActivity
        Intent intent = new Intent();
        intent.putExtra("StatusRun", StopCode);
        intent.setAction("com.zcshou.service.GoGoGoService");
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
        // Log.d(TAG, "setNetworkLocation: " + curLatLng);
        // log.debug(TAG + ": setNetworkLocation: " + curLatLng);

        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));

        String providerStr = LocationManager.NETWORK_PROVIDER;

        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
        } catch (Exception e) {
            Log.d(TAG, "setNetworkLocation error");
            log.debug(TAG + ": setNetworkLocation error");
            e.printStackTrace();
        }
    }

    //set gps location
    private void setGPSLocation() {
        // Log.d(TAG, "setGPSLocation: " + curLatLng);
        // log.debug(TAG + ": setGPSLocation: " + curLatLng);
        String[] latLngStr = curLatLng.split("&");
        LatLng latLng = new LatLng(Double.parseDouble(latLngStr[1]), Double.parseDouble(latLngStr[0]));
        String providerStr = LocationManager.GPS_PROVIDER;

        try {
            locationManager.setTestProviderLocation(providerStr, generateLocation(latLng));
        } catch (Exception e) {
            Log.d(TAG, "setGPSLocation error");
            log.debug(TAG + ": setGPSLocation error");
            e.printStackTrace();
        }
    }

    //remove network provider
    private void rmNetworkTestProvider() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "now remove NetworkProvider");
                log.debug(TAG + ": now remove NetworkProvider");
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            } else {
                Log.d(TAG, "NetworkProvider is not enabled");
                log.debug(TAG + ": NetworkProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "rmNetworkProvider error");
            log.debug(TAG + ": rmNetworkProvider error");
        }
    }

    //set network provider
    private void setNetworkTestProvider() {
        try {
            locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, true, false,
                    false, false, false, false,
                    false, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] success");
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[NETWORK_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[NETWORK_PROVIDER] error");
        }

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "setTestProviderEnabled[NETWORK_PROVIDER] error");
                log.debug(TAG + ": setTestProviderEnabled[NETWORK_PROVIDER] error");
            }
        }

        // 根据 google 的文档，API 29 此方法无效。
        locationManager.setTestProviderStatus(LocationManager.NETWORK_PROVIDER, LocationProvider.AVAILABLE, null,
                System.currentTimeMillis());

    }

    // set GPS provider
    private void rmGPSTestProvider() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d(TAG, "now remove GPSProvider");
                log.debug(TAG + ": now remove GPSProvider");
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } else {
                Log.d(TAG, "GPSProvider is not enabled");
                log.debug(TAG + ": GPSProvider is not enabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "rmGPSProvider error");
            log.debug(TAG + ": rmGPSProvider error");
        }
    }

    private void setGPSTestProvider() {
        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            locationManager.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, false);
            Log.d(TAG, "Disable passive provider");
            log.debug(TAG + ": Disable passive provider");
        }

        if (!locationManager.isProviderEnabled("fused")) {
            locationManager.setTestProviderEnabled("fused", false);
            Log.d(TAG, "Disable fused provider");
            log.debug(TAG + ": Disable fused provider");
        }

        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, true,
                    false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_HIGH);
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] success");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] success");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "addTestProvider[GPS_PROVIDER] error");
            log.debug(TAG + ": addTestProvider[GPS_PROVIDER] error");
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "setTestProviderEnabled[GPS_PROVIDER] error");
                log.debug(TAG + ": setTestProviderEnabled[GPS_PROVIDER] error");
            }
        }

        // 根据 google 的文档，API 29 此方法无效。
        locationManager.setTestProviderStatus(LocationManager.GPS_PROVIDER, LocationProvider.AVAILABLE, null,
                System.currentTimeMillis());
    }

    // set other provider
    private void rmOtherTestProvider() {
        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
            locationManager.setTestProviderEnabled(LocationManager.PASSIVE_PROVIDER, false);
            Log.d(TAG, "Disable passive provider");
            log.debug(TAG + ": Disable passive provider");
        }

        if (!locationManager.isProviderEnabled("fused")) {
            locationManager.setTestProviderEnabled("fused", false);
            Log.d(TAG, "Disable fused provider");
            log.debug(TAG + ": Disable fused provider");
        }
    }


    //uuid random
    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

}


