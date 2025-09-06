package com.fahim.geminiapistarter.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public abstract ChatDao chatDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "gemini_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // optional, for simplicity
                    .build();
        }
        return instance;
    }
}
