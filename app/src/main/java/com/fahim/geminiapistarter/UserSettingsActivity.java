package com.fahim.geminiapistarter;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

public class UserSettingsActivity extends AppCompatActivity {
    
    private UserPreferencesManager preferencesManager;
    private EditText userNameEditText;
    private Switch darkModeSwitch;
    private Switch voiceInputSwitch;
    private TextInputLayout aiPersonalityLayout;
    private EditText aiPersonalityEditText;
    private Button saveButton;
    private Button clearHistoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);
        
        preferencesManager = new UserPreferencesManager(this);
        
        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("User Settings");
        
        userNameEditText = findViewById(R.id.userNameEditText);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        voiceInputSwitch = findViewById(R.id.voiceInputSwitch);
        aiPersonalityLayout = findViewById(R.id.aiPersonalityLayout);
        aiPersonalityEditText = findViewById(R.id.aiPersonalityEditText);
        saveButton = findViewById(R.id.saveButton);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
        
        loadUserPreferences();
        setupClickListeners();
    }
    
    private void loadUserPreferences() {
        UserProfile profile = preferencesManager.getUserProfile();
        
        userNameEditText.setText(profile.getUserName());
        darkModeSwitch.setChecked(profile.isDarkModeEnabled());
        voiceInputSwitch.setChecked(profile.isVoiceInputEnabled());
        aiPersonalityEditText.setText(profile.getAiPersonality());
    }
    
    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> saveUserPreferences());
        
        clearHistoryButton.setOnClickListener(v -> {
            preferencesManager.clearAllData();
            Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
            loadUserPreferences();
        });
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkModeEnabled(isChecked);
            applyTheme(isChecked);
        });
    }
    
    private void saveUserPreferences() {
        String userName = userNameEditText.getText().toString().trim();
        String aiPersonality = aiPersonalityEditText.getText().toString().trim();
        
        if (userName.isEmpty()) {
            userNameEditText.setError("Name cannot be empty");
            return;
        }
        
        if (aiPersonality.isEmpty()) {
            aiPersonality = "helpful";
        }
        
        preferencesManager.setUserName(userName);
        preferencesManager.setVoiceInputEnabled(voiceInputSwitch.isChecked());
        preferencesManager.setAiPersonality(aiPersonality);
        
        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
