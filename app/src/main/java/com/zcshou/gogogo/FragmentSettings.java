package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.elvishew.xlog.XLog;
import com.zcshou.utils.AppUtils;

/* */
public class FragmentSettings extends PreferenceFragmentCompat {

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

        ListPreference pfJoystick = findPreference("joystick_type");
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> {
                CharSequence cs = preference.getEntry();
                String text = cs.toString();
                if (TextUtils.isEmpty(text)) {
                    return "未设置";
                }
                return "当前类型: " + text;
            });
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

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    if(((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                        XLog.d(preference.getKey() + newValue);

                        if (Boolean.parseBoolean(newValue.toString())) {
                            XLog.d("on");
                        } else {
                            XLog.d("off");
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }
    }
}