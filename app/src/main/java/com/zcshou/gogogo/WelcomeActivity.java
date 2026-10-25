package com.zcshou.gogogo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.zcshou.utils.GoUtils;

import java.util.ArrayList;

public class WelcomeActivity extends AppCompatActivity {
    private static SharedPreferences preferences;
    private static final String KEY_ACCEPT_AGREEMENT = "KEY_ACCEPT_AGREEMENT";
    private static final String KEY_ACCEPT_PRIVACY = "KEY_ACCEPT_PRIVACY";

    private static boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final ArrayList<String> ReqPermissions = new ArrayList<>();

    private CheckBox checkBox;
    private Boolean mAgreement;
    private Boolean mPrivacy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        // 生成默认参数的值（一定要尽可能早的调用，因为后续有些界面可能需要使用参数）
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);

        Button startBtn = findViewById(R.id.startButton);
        startBtn.setOnClickListener(v -> startMainActivity());

        checkAgreementAndPrivacy();
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

        // 读取电话状态权限
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ReqPermissions.isEmpty()) {
            isPermission = true;
        } else {
            requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
        }
    }

    private void startMainActivity() {
        if (!checkBox.isChecked()) {
            GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_agreement));
            return;
        }

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
    }

    private void doAcceptation() {
        if (mAgreement && mPrivacy) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
        } else {
            checkBox.setChecked(false);
        }
        //实例化Editor对象
        SharedPreferences.Editor editor = preferences.edit();
        //存入数据
        editor.putBoolean(KEY_ACCEPT_AGREEMENT, mAgreement);
        editor.putBoolean(KEY_ACCEPT_PRIVACY, mPrivacy);
        //提交修改
        editor.apply();
    }

    private void showAgreementDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_agreement);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_agreement_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mAgreement = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mAgreement = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    private void showPrivacyDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.show();
        alertDialog.setCancelable(false);
        Window window = alertDialog.getWindow();
        if (window != null) {
            window.setContentView(R.layout.user_privacy);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimFadeInFadeOut);

            TextView tvContent = window.findViewById(R.id.tv_content);
            Button tvCancel = window.findViewById(R.id.tv_cancel);
            Button tvAgree = window.findViewById(R.id.tv_agree);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(getResources().getString(R.string.app_privacy_content));
            tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            tvContent.setText(ssb, TextView.BufferType.SPANNABLE);

            tvCancel.setOnClickListener(v -> {
                mPrivacy = false;

                doAcceptation();

                alertDialog.cancel();
            });

            tvAgree.setOnClickListener(v -> {
                mPrivacy = true;

                doAcceptation();

                alertDialog.cancel();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void checkAgreementAndPrivacy() {
        preferences = getSharedPreferences(KEY_ACCEPT_AGREEMENT, MODE_PRIVATE);
        mPrivacy = preferences.getBoolean(KEY_ACCEPT_PRIVACY, false);
        mAgreement = preferences.getBoolean(KEY_ACCEPT_AGREEMENT, false);

        checkBox = findViewById(R.id.check_agreement);
        // 拦截 CheckBox 的点击事件
        checkBox.setOnTouchListener((v, event) -> {
            if (v instanceof TextView) {
                TextView text = (TextView) v;
                MovementMethod method = text.getMovementMethod();
                if (method != null && text.getText() instanceof Spannable
                        && event.getAction() == MotionEvent.ACTION_UP) {
                    if (method.onTouchEvent(text, (Spannable) text.getText(), event)) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    }
                }
            }
            return false;
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!mPrivacy || !mAgreement) {
                    GoUtils.DisplayToast(this, getResources().getString(R.string.app_error_read));
                    checkBox.setChecked(false);
                }
            } else {
                mPrivacy = false;
                mAgreement = false;
            }
        });

        String str = getString(R.string.app_agreement_privacy);
        SpannableStringBuilder builder = getSpannableStringBuilder(str);

        checkBox.setText(builder);
        checkBox.setMovementMethod(LinkMovementMethod.getInstance());

        if (mPrivacy && mAgreement) {
            checkBox.setChecked(true);
            checkDefaultPermissions();
            startMainActivity();
        }
    }

    @NonNull
    private SpannableStringBuilder getSpannableStringBuilder(String str) {
        SpannableStringBuilder builder = new SpannableStringBuilder(str);
        ClickableSpan clickSpanAgreement = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showAgreementDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int agreement_start = str.indexOf("《");
        int agreement_end = str.indexOf("》") + 1;
        builder.setSpan(clickSpanAgreement, agreement_start,agreement_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClickableSpan clickSpanPrivacy = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getResources().getColor(R.color.colorPrimary, WelcomeActivity.this.getTheme()));
                ds.setUnderlineText(false);
            }
        };
        int privacy_start = str.indexOf("《", agreement_end);
        int privacy_end = str.indexOf("》", agreement_end) + 1;
        builder.setSpan(clickSpanPrivacy, privacy_start, privacy_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
