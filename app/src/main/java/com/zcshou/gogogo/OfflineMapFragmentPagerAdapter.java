package com.zcshou.gogogo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class OfflineMapFragmentPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private final String[] tabTitles = new String[] {"城市列表", "下载管理"};
    private final Context mContext;

    public OfflineMapFragmentPagerAdapter(FragmentManager fm, int behavior, Context context) {
        super(fm, behavior);
        this.mContext = context;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
        case 1: {
            return FragmentLocalMap.newInstance(position);
        }

        case 0:
        default: {
            return FragmentDownMap.newInstance(position);
        }
        }
    }
    
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
    
    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}