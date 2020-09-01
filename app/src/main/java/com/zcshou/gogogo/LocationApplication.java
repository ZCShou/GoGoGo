package com.zcshou.gogogo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Service;
import android.os.Vibrator;

import com.baidu.mapapi.SDKInitializer;

@SuppressLint("Registered")
public class LocationApplication extends Application {
    public Vibrator mVibrator;
    @Override
    public void onCreate() {
        super.onCreate();
        mVibrator = (Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        SDKInitializer.initialize(getApplicationContext());
    }
}