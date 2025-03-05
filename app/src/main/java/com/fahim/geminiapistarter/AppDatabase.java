package com.fahim.geminiapistarter;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatMessageEntity.class, UserPreferences.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ChatDao chatDao();
    public abstract UserPreferencesDao userPreferencesDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "chat_database"
            ).fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }
}
