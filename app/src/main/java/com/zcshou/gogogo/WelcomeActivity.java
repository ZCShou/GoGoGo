package com.zcshou.gogogo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.zcshou.utils.GoUtils;
import com.zcshou.utils.GoUtils.TimeCount;

import java.util.ArrayList;
import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    private Button startBtn;

    private static SharedPreferences preferences;
    private static final String KEY_IS_FIRST_USAGE = "KEY_IS_FIRST_USAGE";

    private static boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final ArrayList<String> ReqPermissions = new ArrayList<>();

    private static TimeCount mTimer;
    private static final int TIMER_INTERVAL = 1000;
    private static final int TIMER_DURATION = 3000;
    private static final String TIMER_FORMAT = "%d 秒";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        checkFirstStartup();

        initTimer();
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
        mTimer.cancel();

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == SDK_PERMISSION_REQUEST) {
            for (int i = 0; i < ReqPermissions.size(); i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_permission));
                    return;
                }
            }

            isPermission = true;
            mTimer.start();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void checkFirstStartup() {
        preferences = getSharedPreferences(KEY_IS_FIRST_USAGE, MODE_PRIVATE);

        if (preferences.getBoolean(KEY_IS_FIRST_USAGE, true)) {
            showProtocolDialog();
        } else {
            checkDefaultPermissions();
        }
    }

    private void checkDefaultPermissions() {
        // 定位精确位置
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        /*
         * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
         */
        // 读写权限
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

//        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ReqPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }

        // 读取电话状态权限
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ReqPermissions.size() <= 0) {
            isPermission = true;
            mTimer.start();
        } else {
            requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
        }
    }

    private void initTimer() {
        mTimer = new TimeCount(TIMER_DURATION, TIMER_INTERVAL);
        mTimer.setListener(new TimeCount.TimeCountListener() {
            @Override
            public void onTick(long millisUntilFinished) {
                startBtn.setText(String.format(Locale.getDefault(), TIMER_FORMAT, millisUntilFinished / TIMER_INTERVAL));
            }

            @Override
            public void onFinish() {
                startMainActivity();
            }
        });
    }

    private void startMainActivity() {
        if (!GoUtils.isNetworkAvailable(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_network));
            return;
        }

        if (!GoUtils.isGpsOpened(this)) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_gps));
            return;
        }

        if (isPermission) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            WelcomeActivity.this.finish();
        } else {
            checkDefaultPermissions();
        }

        mTimer.cancel();
    }

    private void showProtocolDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_protocol);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            final CheckBox tvCheck = window.findViewById(R.id.tv_check);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_protocol));
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
                    editor.putBoolean(KEY_IS_FIRST_USAGE, false);
                    //提交修改
                    editor.apply();
                }

                if (isPermission) {
                    mTimer.start();
                } else {
                    checkDefaultPermissions();
                }

                alertDialog.cancel();
            });
        }
    }
}
