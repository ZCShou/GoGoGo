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
    private TimeCount mTimer;
    private boolean isNetwork = false;

    private static final String KEY_IS_FIRST_USAGE = "KEY_IS_FIRST_USAGE";
    private SharedPreferences preferences;

    private static boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final ArrayList<String> ReqPermissions = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.welcome);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        /* 定时器 */
        int cnt = Integer.parseInt(getResources().getString(R.string.welcome_btn_cnt));
        mTimer = new TimeCount(cnt, 1000);
        mTimer.setListener(new TimeCount.TimeCountListener() {
            @Override
            public void onTick(long millisUntilFinished) {
                startBtn.setText(String.format(Locale.getDefault(), "%d秒", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                startMainActivity();
            }
        });

        startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        if (!GoUtils.isNetworkAvailable(this)) {
            startBtn.setText(getResources().getString(R.string.welcome_network_error));
        } else {
            isNetwork = true;
            preferences = getSharedPreferences(KEY_IS_FIRST_USAGE, MODE_PRIVATE);

            if (preferences.getBoolean(KEY_IS_FIRST_USAGE, true)) {
                showProtocolDialog();
            } else {
                checkDefaultPermissions();
            }
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
        mTimer.cancel();

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
                mTimer.start();
            } else {
                startBtn.setText(getResources().getString(R.string.welcome_permission_error));
                startBtn.setClickable(true);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private void startMainActivity() {
        if (isNetwork) {
            if (isPermission) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
                WelcomeActivity.this.finish();
            } else {
                checkDefaultPermissions();
            }
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
            ssb.append(getResources().getString(R.string.protocol));
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
