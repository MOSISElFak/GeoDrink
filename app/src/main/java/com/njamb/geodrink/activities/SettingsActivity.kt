package com.njamb.geodrink.activities

import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment

import com.njamb.geodrink.R


class SettingsActivity : AppCompatPreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager
                .beginTransaction()
                .replace(android.R.id.content, GeneralSettingsFragment())
                .commit()
    }

    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.preference_headers, target)
    }


    class GeneralSettingsFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.general_preferences)
        }
    }
}
