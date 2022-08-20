package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.elvishew.xlog.XLog;
import com.zcshou.utils.GoUtils;

import java.util.Objects;

public class FragmentSettings extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);

        // 设置版本号
        String verName;
        verName = GoUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion != null) {
            pfVersion.setSummary(verName);
        }

        ListPreference pfJoystick = findPreference("setting_joystick_type");
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> "当前类型: " + Objects.requireNonNull(preference.getEntry()));
            pfJoystick.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().equals(""));
        }

        EditTextPreference pfWalk = findPreference("setting_walk");
        if (pfWalk != null) {
            // 使用自定义 SummaryProvider
            pfWalk.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> "当前值: " + preference.getText());
            pfWalk.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                Selection.setSelection(editText.getText(), editText.length());
            });
            pfWalk.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().equals(""));
        }

        EditTextPreference pfRun = findPreference("setting_run");
        if (pfRun != null) {
            pfRun.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> "当前值: " + preference.getText());
            pfRun.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                Selection.setSelection(editText.getText(), editText.length());
            });
            pfRun.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().equals(""));
        }
        EditTextPreference pfBike = findPreference("setting_bike");
        if (pfBike != null) {
            pfBike.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> "当前值: " + preference.getText());
            pfBike.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                Selection.setSelection(editText.getText(), editText.length());
            });
            pfBike.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().equals(""));
        }

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
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
            });
        }

        EditTextPreference pfPosHisValid = findPreference("setting_pos_history");
        if (pfPosHisValid != null) {
            // 使用自定义 SummaryProvider
            pfPosHisValid.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> "当前值: " + preference.getText());
            pfPosHisValid.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                Selection.setSelection(editText.getText(), editText.length());
            });
            pfPosHisValid.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().equals(""));
        }
    }
}