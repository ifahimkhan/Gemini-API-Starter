package com.fahim.geminiapistarter;

import android.app.Application;

public class App extends Application {

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Apply saved dark mode on app start
        ThemeToggleHelper.applySavedTheme();
    }

    public static App getAppContext() {
        return instance;
    }
}
