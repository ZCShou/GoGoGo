package com.zcshou.gogogo;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.EditTextPreference;
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
        if (pfVersion != null) {
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

        EditTextPreference pfWalk = findPreference("setting_walk");
        if (pfWalk != null) {
            pfWalk.setSummaryProvider(new Preference.SummaryProvider<EditTextPreference>() {// 使用自定义 SummaryProvider
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "未设置";
                    }
                    return "当前值: " + text;
                }
            });
            pfWalk.setOnBindEditTextListener(// 自定义 EditTextPreference 对话框
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                        }
                    });
        }

        EditTextPreference pfRun = findPreference("setting_run");
        if (pfRun != null) {
            pfRun.setSummaryProvider(new Preference.SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "未设置";
                    }
                    return "当前值: " + text;
                }
            });
            pfRun.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                        }
                    });
        }
        EditTextPreference pfBike = findPreference("setting_bike");
        if (pfBike != null) {
            pfBike.setSummaryProvider(new Preference.SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    String text = preference.getText();
                    if (TextUtils.isEmpty(text)) {
                        return "未设置";
                    }
                    return "当前值: " + text;
                }
            });
            pfBike.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                        }
                    });
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

}