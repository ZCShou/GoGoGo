package com.zcshou.gogogo;

import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference.OnPreferenceChangeListener;

/* */
public class FragmentSettings extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
    
    //    override fun onPreferenceTreeClick(preference: Preference): Boolean {
    //        return when (preference.key) {
    //            getString(R.string.pref_key_allow_notification) -> {
    //                true
    //            }
    //            getString(R.string.pref_key_zipcode) -> {
    //                true
    //            }
    //            getString(R.string.pref_key_unit) -> {
    //                true
    //            }
    //            else -> {
    //                super.onPreferenceTreeClick(preference)
    //            }
    //        }
    //    }
}