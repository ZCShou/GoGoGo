package com.zcshou.gogogo;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 临时处理，因为没有找到其他方法处理 填充 StatusBar
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary, this.getTheme()));

        setContentView(R.layout.activity_settings);
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings, new FragmentSettings())
            .commit();
        }

        /* 获取默认的顶部的标题栏（安卓称为 ActionBar）*/
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish(); // back button
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}