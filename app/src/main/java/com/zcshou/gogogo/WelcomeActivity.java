package com.zcshou.gogogo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.os.SystemClock;
import android.view.View;
import android.widget.Button;

import com.zcshou.service.GoSntpClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WelcomeActivity extends AppCompatActivity {
    private Button startBtn;
    private TimeCount time;
    private static final long mTS = 1630972800;
    int cnt;
    boolean isPermission;
    boolean isLimit;
    static final  int SDK_PERMISSION_REQUEST = 127;
    ArrayList<String> ReqPermissions = new ArrayList<>();
    //private Date mDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 从登录界面进入主页，按home键回桌面再进入app，重新弹出登录界面的问题
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.welcome);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        isPermission = false;
        isLimit = true;

        cnt = Integer.parseInt(getResources().getString (R.string.welcome_btn_cnt));
        time = new TimeCount(cnt, 1000);
        startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time.cancel();
                startMainActivity();
            }
        });

        startBtn.setClickable(false);        // 放在 setOnClickListener 之后才能生效

        if (isNetworkAvailable()) {
            TimeTask timeTask = new TimeTask();

            ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
            threadExecutor.submit(timeTask);
        } else {
            startBtn.setClickable(true);
            startBtn.setText("网络不可用");
        }

        requestNeedPermissions();
    }

    //WIFI是否可用
    private boolean isWifiConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.isAvailable();
        }

        return false;
    }

    //MOBILE网络是否可用
    private boolean isMobileConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mMobileNetworkInfo = mConnectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (mMobileNetworkInfo != null) {
            return mMobileNetworkInfo.isAvailable();
        }

        return false;
    }

    // 断是否有网络连接，但是如果该连接的网络无法上网，也会返回true
    public boolean isNetworkConnected() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();

        if (mNetworkInfo != null) {
            return mNetworkInfo.isAvailable();
        }

        return false;
    }

    //网络是否可用
    private boolean isNetworkAvailable() {
        return ((isWifiConnected() || isMobileConnected()) && isNetworkConnected());
    }

    private void startMainActivity() {
        if (isPermission && !isLimit) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            //if (mDate == null) {
            //    intent.putExtra("DT", 0);
            //} else {
            //    intent.putExtra("DT", mDate.getTime() / 1000);
            //}
            startActivity(intent);
        }
        WelcomeActivity.this.finish();
    }

    @TargetApi(23)
    private void requestNeedPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            //悬浮窗
            // if (checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
            //     permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
            // }

            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

             // 读取电话状态权限
            if (checkSelfPermission( Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                ReqPermissions.add(Manifest.permission.READ_PHONE_STATE);
            }

            if (ReqPermissions.size() > 0) {
                requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
            } else {
                isPermission = true;
            }
        } else {
            isPermission = true;
        }
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SDK_PERMISSION_REQUEST) {
            int i;
            for (i = 0; i < ReqPermissions.size(); i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    break;// Permission Denied 权限被拒绝
                }
            }

            if (i >= ReqPermissions.size()) {
                isPermission = true;
            } else {
                isPermission = false;
                startBtn.setText("权限不足");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    class TimeCount extends CountDownTimer {
        public TimeCount(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);//参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish() {//计时完毕时触发
            startMainActivity();
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示
            startBtn.setText(String.format(Locale.getDefault(), "%d秒", millisUntilFinished / 1000));
        }
    }

    private class TimeTask implements Runnable {
        private String[] ntpServerPool = {"ntp1.aliyun.com", "ntp2.aliyun.com", "ntp3.aliyun.com", "ntp4.aliyun.com", "ntp5.aliyun.com", "ntp6.aliyun.com", "ntp7.aliyun.com",
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
                    //mDate = new Date(now);
                    if (now /1000 < mTS) {
                        isLimit = false;
                    }
                    break;
                }
            }
            if (i < ntpServerPool.length) {
                time.start();
            }
            startBtn.setClickable(true);
        }
    }

}
