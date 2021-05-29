package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.zcshou.utils.AppUtils;

/* */
public class FragmentSettings extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

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

        EditTextPreference pfWalk = findPreference("setting_walk");
        if (pfWalk != null) {
            // 使用自定义 SummaryProvider
            pfWalk.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    return "未设置";
                }
                return "当前值: " + text;
            });
            pfWalk.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER));
        }

        EditTextPreference pfRun = findPreference("setting_run");
        if (pfRun != null) {
            pfRun.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    return "未设置";
                }
                return "当前值: " + text;
            });
            pfRun.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER));
        }
        EditTextPreference pfBike = findPreference("setting_bike");
        if (pfBike != null) {
            pfBike.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    return "未设置";
                }
                return "当前值: " + text;
            });
            pfBike.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER));
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

}