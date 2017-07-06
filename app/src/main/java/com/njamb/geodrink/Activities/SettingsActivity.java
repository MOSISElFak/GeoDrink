package com.njamb.geodrink.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.njamb.geodrink.R;
import com.njamb.geodrink.Services.BackgroundService;

import java.util.List;


public class SettingsActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new GeneralSettingsFragment())
                .commit();

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        if (key.equals("pref_service")) {
                            Intent intent = new Intent(SettingsActivity.this, BackgroundService.class);
                            if (!sharedPreferences.getBoolean("pref_service", true)) {
                                stopService(intent);
                            }
                            else {
                                startService(intent);
                            }
                        }
                    }
                });
    }

    @Override
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }


    public static class GeneralSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.general_preferences);
        }
    }
}
