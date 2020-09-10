package com.zcshou.gogogo;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.zcshou.utils.AppUtils;
import com.zcshou.utils.DeviceIdUtils;

/* */
public class FragmentSettings extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);

        // 设置版本号
        String verName;
        verName = AppUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion  != null) {
            pfVersion.setSummary(verName);
        }

        // 设置作者
        Preference pfAuthor = findPreference("setting_author");
        if (pfAuthor != null) {
            pfAuthor.setSummary(R.string.author);
        }

        // 设置 ID
        String sDid;
        sDid = DeviceIdUtils.getDeviceId(FragmentSettings.this.getContext());
        Preference pfDid = findPreference("setting_device_id");
        if (pfDid != null) {
            pfDid.setSummary(sDid);
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

}