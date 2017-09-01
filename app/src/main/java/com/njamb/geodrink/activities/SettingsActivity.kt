package com.njamb.geodrink.activities

import android.app.FragmentManager
import android.app.FragmentTransaction
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment

import com.njamb.geodrink.R


inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
    val fragmentTransaction = beginTransaction()
    fragmentTransaction.func()
    fragmentTransaction.commit()
}

class SettingsActivity : AppCompatPreferenceActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager?.inTransaction {
            replace(android.R.id.content, GeneralSettingsFragment())
        }
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
