package com.c168.aaryahi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = getSharedPreferences("user_settings", MODE_PRIVATE);
        int currentThemeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(currentThemeMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RadioGroup themeRadioGroup = findViewById(R.id.theme_radio_group);
        SwitchMaterial switchReopenChat = findViewById(R.id.switch_reopen_chat);

        // Set initial state from preferences
        if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themeRadioGroup.check(R.id.radio_light);
        } else if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeRadioGroup.check(R.id.radio_dark);
        } else {
            themeRadioGroup.check(R.id.radio_system);
        }
        switchReopenChat.setChecked(prefs.getBoolean("reopen_last_chat", true));

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int newMode = currentMode;

            if (checkedId == R.id.radio_light) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radio_dark) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else if (checkedId == R.id.radio_system) {
                newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            if (newMode != currentMode) {
                prefs.edit().putInt("theme_mode", newMode).commit();
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        switchReopenChat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("reopen_last_chat", isChecked).commit();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
