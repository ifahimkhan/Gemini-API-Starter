package com.fahim.geminiapistarter;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    private static final String PREFS_NAME = "gemini_app_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FONT_SIZE = "font_size";

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public void setDarkMode(boolean darkMode) {
        editor.putBoolean(KEY_DARK_MODE, darkMode);
        editor.apply();
    }

    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setUsername(String username) {
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "User");
    }

    public void setFontSize(int fontSize) {
        editor.putInt(KEY_FONT_SIZE, fontSize);
        editor.apply();
    }

    public int getFontSize() {
        return preferences.getInt(KEY_FONT_SIZE, 18); // Default font size
    }
}