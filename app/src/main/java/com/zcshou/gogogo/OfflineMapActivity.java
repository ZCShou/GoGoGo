package com.zcshou.gogogo;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class OfflineMapActivity extends FragmentActivity {

    ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("OfflineMapActivity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_map);

        OfflineMapFragmentPagerAdapter pagerAdapter = new OfflineMapFragmentPagerAdapter(getSupportFragmentManager(),
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,this);

        ViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = findViewById(R.id.sliding_tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        btnBack = findViewById(R.id.back);
        // btnBack.setImageResource(R.drawable.ic_run);

    }

    public void goHome(View view) {
        this.finish(); // back button
    }


    @Override
    public void onDestroy() {
        Log.d("OfflineMapActivity", "onDestroy");

        super.onDestroy();
    }
    
    @Override
    public void onStop() {
        Log.d("OfflineMapActivity", "onStop");
        super.onStop();
    }
}
