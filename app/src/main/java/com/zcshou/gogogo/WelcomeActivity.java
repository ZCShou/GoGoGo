package com.zcshou.gogogo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.zcshou.service.GoSntpClient;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WelcomeActivity extends BaseActivity {
    private Button startBtn;
    private TimeCount time;
    private static final long mTS = 1609286401;
    private boolean isPermission;
    private boolean isLimit;
    private boolean isNetwork;
    private static final int SDK_PERMISSION_REQUEST = 127;
    ArrayList<String> ReqPermissions = new ArrayList<>();
    private boolean isFirstUse;
    private SharedPreferences preferences;
    ExecutorService threadExecutor;

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

        int cnt = Integer.parseInt(getResources().getString(R.string.welcome_btn_cnt));
        time = new TimeCount(cnt, 1000);
        startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        startBtn.setClickable(false);        // 放在 setOnClickListener 之后才能生效

        if (isNetworkAvailable()) {
            TimeTask timeTask = new TimeTask();
            threadExecutor = Executors.newSingleThreadExecutor();
            threadExecutor.submit(timeTask);
            isNetwork = true;
        } else {
            startBtn.setClickable(true);
            startBtn.setText("网络不可用");
            isNetwork = false;
        }

        preferences = getSharedPreferences("isFirstUse", MODE_PRIVATE);
        isFirstUse = preferences.getBoolean("isFirstUse", true);

        if (isFirstUse) {
            showProtocolDialog();
        } else {
            requestNeedPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        time.cancel();
        threadExecutor.shutdownNow();

        super.onDestroy();
    }

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
                if (!isLimit) {
                    time.start();
                }
            } else {
                isPermission = false;
                startBtn.setText("权限不足");
                startBtn.setClickable(true);
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
                if (!isLimit) {
                    time.start();
                }
            }
        } else {
            isPermission = true;
            if (!isLimit) {
                time.start();
            }
        }
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
            startActivity(intent);
        }
        time.cancel();
        threadExecutor.shutdownNow();
        WelcomeActivity.this.finish();
    }

    private void showProtocolDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.welcom_protocol);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            TextView tvCancel = window.findViewById(R.id.tv_cancel);
            TextView tvAgree = window.findViewById(R.id.tv_agree);
            final CheckBox tvCheck = window.findViewById(R.id.tv_check);
            String str = "1. 本软件专为学习 Android 开发使用，不会收集任何用户数据。"
                    + "严禁利用本软件侵犯他人隐私权或者用于游戏牟利，如软件使用者不能遵守此规定， 请立即删除。"
                    + "对于因用户使用本软件而造成自身或他人隐私泄露等任何不良后果，均由用户自行承担，软件作者不负任何责任。\n"
                    + "2. 用户不得对本软件产品进行反向工程（reverse engineer）、反向编译（decompile）或反汇编（disassemble）， 违者属于侵权行为，并自行承担由此产生的不利后果。\n"
                    + "3. 软件保证不含任何病毒，木马，等破坏用户数据的恶意代码，但是由于本软件产品可以通过网络等途径下载、传播，对于从非软件作者指定站点下载的本软件产品软件作者无法保证该软件是否感染计算机病毒、是否隐藏有伪装的特洛伊木马程序或者黑客软件，不承担由此引起的直接和间接损害责任。\n"
                    + "4. 软件会不断更新，以便及时为用户提供新功能和修正软件中的BUG。 同时软件作者保证本软件在升级过程中也不含有任何旨在破坏用户计算机数据的恶意代码。\n"
                    + "5. 由于用户计算机软硬件环境的差异性和复杂性，本软件所提供的各项功能并不能保证在任何情况下都能正常执行或达到用户所期望的结果。 用户使用本软件所产生的一切后果，软件作者不承担任何责任。\n"
                    + "6. 如果用户自行安装本软件，即表明用户信任软件作者，自愿选择安装本软件，并接受本协议所有条款。 如果用户不接受本协议，请立即删除。\n";
 
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(str);

            // final int start = str.indexOf("《");//第一个出现的位置
            // ssb.setSpan(new ClickableSpan() {
            //     @Override
            //     public void onClick(@NonNull View widget) {
            //         //Toast.makeText(SplashScreenActivity.this, "《隐私政策》", Toast.LENGTH_SHORT).show();
            //     }

            //     @Override
            //     public void updateDrawState(@NonNull TextPaint ds) {
            //         super.updateDrawState(ds);
            //         ds.setColor(getResources().getColor(R.color.chocolate));
            //         ds.setUnderlineText(false);
            //     }
            // }, start, start + 6, 0);

            // int end = str.lastIndexOf("《");
            // ssb.setSpan(new ClickableSpan() {
            //     @Override
            //     public void onClick(@NonNull View widget) {
            //         // Toast.makeText(SplashScreenActivity.this, "《用户协议》", Toast.LENGTH_SHORT).show();
            //     }

            //     @Override
            //     public void updateDrawState(@NonNull TextPaint ds) {
            //         super.updateDrawState(ds);
            //         ds.setColor(getResources().getColor(R.color.chocolate));
            //         ds.setUnderlineText(false);
            //     }
            // }, end, end + 6, 0);
 
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                alertDialog.cancel();
                finish();
            });
 
            tvAgree.setOnClickListener(v -> {
                if (tvCheck.isChecked()) {
                    //实例化Editor对象
                    SharedPreferences.Editor editor = preferences.edit();
                    //存入数据
                    editor.putBoolean("isFirstUse", false);
                    //提交修改
                    editor.apply();

                    isFirstUse = false;
                }

                requestNeedPermissions();

                alertDialog.cancel();
            });
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
            for (String s : ntpServerPool) {
                if (GoSntpClient.requestTime(s, 30000)) {
                    long now = GoSntpClient.getNtpTime() + SystemClock.elapsedRealtime() - GoSntpClient.getNtpTimeReference();
                    if (now / 1000 < mTS) {
                        isLimit = false;
                    }
                    break;
                }
            }
            if (!isLimit) {
                if (!isFirstUse && isPermission && isNetwork) {
                    time.start();
                }
            } else {
                startBtn.setClickable(true);
                startBtn.setText("无法连接服务器");
            }
        }
    }

}
