package com.zcshou.gogogo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {
    private Button startBtn;
    private TimeCount time;
    int cnt;
    boolean isPermission;
    static final  int SDK_PERMISSION_REQUEST = 127;
    ArrayList<String> ReqPermissions = new ArrayList<>();

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

        cnt = Integer.parseInt(getResources().getString (R.string.welcome_btn_cnt));
        time = new TimeCount(cnt, 1000);
        startBtn = findViewById(R.id.startButton);
        //startBtn.setClickable(false);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                time.cancel();
                startMainActivity();
            }
        });

        requestNeedPermissions();
    }
    
    private void startMainActivity() {
        if (isPermission) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            WelcomeActivity.this.finish();
        }
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
                time.start();
                isPermission = true;
            }
        } else {
            time.start();
            isPermission = true;
        }
    }

//    @TargetApi(23)
//    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
//        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
//            if (shouldShowRequestPermissionRationale(permission)) {
//                return true;
//            } else {
//                permissionsList.add(permission);
//                return false;
//            }
//        } else {
//            return true;
//        }
//    }

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
                time.start();
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
            // startBtn.setText(getResources().getString (R.string.welcome_btn_txt));
            // startBtn.setClickable(true);
        }

        @Override
        public void onTick(long millisUntilFinished) { //计时过程显示
            startBtn.setText(String.format(Locale.getDefault(), "%d秒", millisUntilFinished / 1000));
        }
    }

}
