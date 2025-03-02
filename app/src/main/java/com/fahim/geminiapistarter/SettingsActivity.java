package com.fahim.geminiapistarter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NavUtils;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Add the fragment to the activity
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        // Set up the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Set up font size preference summary
            ListPreference fontSizePreference = findPreference("font_size");
            if (fontSizePreference != null) {
                // Set initial summary
                String value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("font_size", "medium");
                fontSizePreference.setSummary(value);
            }

            // Set up username preference
            EditTextPreference usernamePreference = findPreference("username");
            if (usernamePreference != null) {
                // Set initial summary
                String value = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString("username", getString(R.string.default_username));
                usernamePreference.setSummary(value);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("dark_mode")) {
                boolean darkMode = sharedPreferences.getBoolean(key, false);
                AppCompatDelegate.setDefaultNightMode(
                        darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(getContext(), "Theme updated", Toast.LENGTH_SHORT).show();
            } else if (key.equals("font_size")) {
                ListPreference fontSizePreference = findPreference(key);
                if (fontSizePreference != null) {
                    fontSizePreference.setSummary(sharedPreferences.getString(key, "medium"));
                }
            } else if (key.equals("username")) {
                EditTextPreference usernamePreference = findPreference(key);
                if (usernamePreference != null) {
                    usernamePreference.setSummary(sharedPreferences.getString(key, getString(R.string.default_username)));
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }
}