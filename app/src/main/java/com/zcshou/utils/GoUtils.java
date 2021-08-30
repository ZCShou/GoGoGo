package com.zcshou.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
//import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoUtils {
    //WIFI是否可用
    public static boolean isWifiConnected(Context context) {
//        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
//
//        if (mWiFiNetworkInfo != null && mWiFiNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//            return mWiFiNetworkInfo.isAvailable();
//        }
//        return false;

        // 从 API 29 开始，NetworkInfo 被标记为过时
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    //MOBILE网络是否可用
    public static boolean isMobileConnected(Context context) {
//        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo mMobileNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
//
//        if (mMobileNetworkInfo != null && mMobileNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
//            return mMobileNetworkInfo.isAvailable();
//        }
//
//        return false;

        // 从 API 29 开始，NetworkInfo 被标记为过时
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) {
            return false;
        }
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    // 断是否有网络连接，但是如果该连接的网络无法上网，也会返回true
    public static boolean isNetworkConnected(Context context) {
//        ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
//
//        if (mNetworkInfo != null) {
//            return mNetworkInfo.isConnected();
//        }
//
//        return false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    //网络是否可用
    public static boolean isNetworkAvailable(Context context) {
        return ((isWifiConnected(context) || isMobileConnected(context)) && isNetworkConnected(context));
    }

    //判断GPS是否打开
    public static  boolean isGpsOpened(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    //模拟位置权限是否开启
    public static boolean isAllowMockLocation(Context context) {
        boolean canMockPosition = false;

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);//获得LocationManager引用
            LocationProvider provider = locationManager.getProvider(LocationManager.GPS_PROVIDER);

            // 为防止在已有testProvider的情况下导致addTestProvider抛出异常，先移除testProvider
            try {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (provider != null) {
                try {
                    locationManager.addTestProvider(
                            provider.getName()
                            , provider.requiresNetwork()
                            , provider.requiresSatellite()
                            , provider.requiresCell()
                            , provider.hasMonetaryCost()
                            , provider.supportsAltitude()
                            , provider.supportsSpeed()
                            , provider.supportsBearing()
                            , provider.getPowerRequirement()
                            , provider.getAccuracy());
                    canMockPosition = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    locationManager.addTestProvider(
                            LocationManager.GPS_PROVIDER
                            , true, true, false, false, true, true, true
                            , Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
                    canMockPosition = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 模拟位置可用
            if (canMockPosition) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
                //remove test provider
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return canMockPosition;
    }

    /**
     * [获取应用程序版本名称]
     * @param context context
     * @return 当前应用的版本名称
     */
    public static synchronized String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();

            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);

            return packageInfo.versionName;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取App的名称
     *
     * @param context 上下文
     *
     * @return 名称
     */
    public static String getAppName(Context context) {
        PackageManager pm = context.getPackageManager();
        //获取包信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            //获取应用 信息
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            //获取albelRes
            int labelRes = applicationInfo.labelRes;
            //返回App的名称
            return context.getResources().getString(labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static  String timeStamp2Date(String seconds) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        return sdf.format(new Date(Long.parseLong(seconds + "000")));
    }

    /* 计数器类 */
    public static class TimeCount extends CountDownTimer {
        private TimeCountListener mListener;

        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            mListener.onFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示
            mListener.onTick(millisUntilFinished);
        }

        public void setListener(TimeCountListener mListener) {
            this.mListener = mListener;
        }

        public interface TimeCountListener {
            void onTick(long millisUntilFinished);
            void onFinish();
        }
    }
}