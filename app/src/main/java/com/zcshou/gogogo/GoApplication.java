package com.zcshou.gogogo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Service;
import android.os.Vibrator;

import com.baidu.mapapi.SDKInitializer;
import com.google.android.gms.ads.MobileAds;

@SuppressLint("Registered")
public class GoApplication extends Application {
    public Vibrator mVibrator;
    @Override
    public void onCreate() {
        super.onCreate();
        mVibrator = (Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        // 百度地图 SDK 初始化
        SDKInitializer.initialize(getApplicationContext());
        // 谷歌广告 SDK 初始化
        MobileAds.initialize(getApplicationContext(), initializationStatus -> {
        });
    }
}