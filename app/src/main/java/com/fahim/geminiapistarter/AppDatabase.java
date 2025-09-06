package com.fahim.geminiapistarter;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatMessage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ChatMessageDao messageDao();
    private static volatile AppDatabase INSTANCE;

    static AppDatabase getDb(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "chat_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}