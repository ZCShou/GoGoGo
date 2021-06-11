package com.zcshou.gogogo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.zcshou.utils.GoUtils;

import java.util.Locale;

public class WelcomeActivity extends BaseActivity {
    private Button startBtn;
    private TimeCount time;
    private boolean isNetwork = false;

    private static final String KEY_IS_FIRST_USAGE = "KEY_IS_FIRST_USAGE";
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        /* 全屏，必须尽早调用 */
//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
        // 从登录界面进入主页，按home键回桌面再进入app，重新弹出登录界面的问题
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.welcome);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        int cnt = Integer.parseInt(getResources().getString(R.string.welcome_btn_cnt));
        time = new TimeCount(cnt, 1000);
        startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());
        startBtn.setClickable(false);        // 放在 setOnClickListener 之后才能生效

        if (!GoUtils.isNetworkAvailable(this)) {
            startBtn.setText(getResources().getString(R.string.welcome_network_error));
            startBtn.setClickable(true);
        } else {
            isNetwork = true;
            preferences = getSharedPreferences(KEY_IS_FIRST_USAGE, MODE_PRIVATE);

            if (preferences.getBoolean(KEY_IS_FIRST_USAGE, true)) {
                showProtocolDialog();
            } else {
                if (haveDefaultPermissions()) {
                    time.start();
                } else {
                    requestDefaultPermissions();
                }
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
        time.cancel();

        super.onDestroy();
    }

    @Override
    public void onPermissionsIsOK(boolean isOK) {       /* 重写父类的方法，实现与父类通信 */
        if (isOK) {
            time.start();
        } else {
            startBtn.setText(getResources().getString(R.string.welcome_permission_error));
            startBtn.setClickable(true);
        }
        super.onPermissionsIsOK(isOK);
    }

    private void startMainActivity() {
        if (isNetwork) {
            if (haveDefaultPermissions()) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                startActivity(intent);
                WelcomeActivity.this.finish();
            } else {
                requestDefaultPermissions();
            }
        }
        time.cancel();
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

                if (haveDefaultPermissions()) {
                    time.start();
                } else {
                    requestDefaultPermissions();
                }

                alertDialog.cancel();
            });
        }
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
}
