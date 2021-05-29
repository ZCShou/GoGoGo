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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.zcshou.gogogo.MainActivity;
import com.zcshou.gogogo.R;
import com.zcshou.joystick.JoyStick;

public class ServiceGo extends Service {
    // 定位相关变量
    private static final int HANDLER_MSG_ID = 0;
    private static final String SERVICE_GO_HANDLER_NAME = "ServiceGoLocation";
    private LocationManager locationManager;
    private HandlerThread handlerThread;
    private Handler handler;
    private double curLat = 36.667662;
    private double curLng = 117.027707;
    // 摇杆相关
    private JoyStick mJoyStick;
    // 通知栏消息
    private static final int SERVICE_GO_NOTE_ID = 1;
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW = "ShowJoyStick";
    private static final String SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE = "HideJoyStick";
    private static final String SERVICE_GO_NOTE_CHANNEL_ID = "SERVICE_GO_NOTE";
    private static final String SERVICE_GO_NOTE_CHANNEL_NAME = "SERVICE_GO_NOTE";
    private NoteActionReceiver acReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        removeTestProviderNetwork();
        removeTestProviderGPS();

        addTestProviderNetwork();
        addTestProviderGPS();

        initGoLocation();

        initNotification();

        initJoyStick();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String curLatLng = intent.getStringExtra("CurLatLng");
        if (curLatLng != null) {
            String[] latLngStr = curLatLng.split("&");
            curLng = Double.parseDouble(latLngStr[0]);
            curLat = Double.parseDouble(latLngStr[1]);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mJoyStick.hide();

        handlerThread.quit();
        handler.removeMessages(HANDLER_MSG_ID);

        removeTestProviderNetwork();
        removeTestProviderGPS();

        unregisterReceiver(acReceiver);
        stopForeground(true);

        super.onDestroy();
    }

    private void initNotification() {
        acReceiver = new NoteActionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW);
        filter.addAction(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE);
        registerReceiver(acReceiver, filter);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification;
        //准备intent
        Intent clickIntent = new Intent(this, MainActivity.class);
        PendingIntent clickPI = PendingIntent.getActivity(this, 1, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent showIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW);
        PendingIntent showPendingPI = PendingIntent.getBroadcast(this, 0, showIntent, PendingIntent.FLAG_CANCEL_CURRENT );
        Intent hideIntent = new Intent(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE);
        PendingIntent hidePendingPI = PendingIntent.getBroadcast(this, 0, hideIntent, PendingIntent.FLAG_CANCEL_CURRENT );

        NotificationChannel mChannel = new NotificationChannel(SERVICE_GO_NOTE_CHANNEL_ID, SERVICE_GO_NOTE_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(mChannel);
        }

        notification = new NotificationCompat.Builder(this, SERVICE_GO_NOTE_CHANNEL_ID)
                .setChannelId(SERVICE_GO_NOTE_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.app_service))
                .setContentIntent(clickPI)
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_show), showPendingPI))
                .addAction(new NotificationCompat.Action(null, getResources().getString(R.string.note_hide), hidePendingPI))
                .setSmallIcon(R.mipmap.ic_launcher).build();

        startForeground(SERVICE_GO_NOTE_ID, notification);
    }

    private void initJoyStick() {
        mJoyStick = new JoyStick(this);
        mJoyStick.setListener((disLng, disLat) -> {
            // 根据当前的经纬度和距离，计算下一个经纬度
            // Latitude: 1 deg = 110.574 km // 纬度的每度的距离大约为 110.574km
            // Longitude: 1 deg = 111.320*cos(latitude) km  // 经度的每度的距离从0km到111km不等
            // 具体见：http://wp.mlab.tw/?p=2200
            curLng += disLng / (111.320 * Math.cos(Math.abs(curLat) * Math.PI / 180));
            curLat += disLat / 110.574;
        });
        mJoyStick.show();
    }

    private void initGoLocation() {
        // 创建 HandlerThread 实例，第一个参数是线程的名字
        handlerThread = new HandlerThread(SERVICE_GO_HANDLER_NAME, Process.THREAD_PRIORITY_FOREGROUND);
        // 启动 HandlerThread 线程
        handlerThread.start();
        // Handler 对象与 HandlerThread 的 Looper 对象的绑定
        handler = new Handler(handlerThread.getLooper()) {
            // 这里的Handler对象可以看作是绑定在HandlerThread子线程中，所以handlerMessage里的操作是在子线程中运行的
            public void handleMessage(@NonNull Message msg) {
                try {
                    Thread.sleep(80);

                    setNetworkLocation();
                    setGPSLocation();

                    sendEmptyMessage(HANDLER_MSG_ID);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        };

        handler.sendEmptyMessage(HANDLER_MSG_ID);
    }

    // 生成一个位置
    public Location makeLocation() {
        Location loc = new Location(LocationManager.GPS_PROVIDER);  // 这里只能填写 GPS
        loc.setAccuracy(Criteria.ACCURACY_FINE);                  // 设定此位置的估计水平精度，以米为单位。
        loc.setAltitude(55.0D);                 // 设置高度，在 WGS 84 参考坐标系中的米
        loc.setBearing(1.0F);                   // 方向（度）
        loc.setLatitude(curLat);            // 纬度（度）
        loc.setLongitude(curLng);           // 经度（度）
        loc.setTime(System.currentTimeMillis());    // 本地时间
        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        Bundle bundle = new Bundle();
        bundle.putInt("satellites", 7);
        loc.setExtras(bundle);

        return loc;
    }

    private void removeTestProviderGPS() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTestProviderGPS() {
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, true,
                    false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_MEDIUM);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setGPSLocation() {
        try {
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, makeLocation());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeTestProviderNetwork() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        }
    }

    private void setNetworkLocation() {
        try {
            locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, makeLocation());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class NoteActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW)) {
                    mJoyStick.show();
                }

                if (action.equals(SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE)) {
                    mJoyStick.hide();
                }
            }
        }
    }
}


