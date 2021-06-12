package com.zcshou.gogogo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class BaseActivity extends AppCompatActivity {
    private static boolean isPermission = false;
    private static final int SDK_PERMISSION_REQUEST = 127;
    private static final ArrayList<String> ReqPermissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* 防止旋转(这里效果不不好，在这换界面的瞬间还是会出现屏幕旋转) */
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        checkDefaultPermissions();
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
                onPermissionsIsOK(true);
            } else {
                onPermissionsIsOK(false);
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
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ReqPermissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (ReqPermissions.size() <= 0) {
            isPermission = true;
        }
    }

    public void requestDefaultPermissions() {
        if (ReqPermissions.size() > 0) {
            requestPermissions(ReqPermissions.toArray(new String[0]), SDK_PERMISSION_REQUEST);
        }
    }

    public boolean haveDefaultPermissions() {
        return isPermission;
    }

    protected void onPermissionsIsOK(boolean isOK) {

    }
}