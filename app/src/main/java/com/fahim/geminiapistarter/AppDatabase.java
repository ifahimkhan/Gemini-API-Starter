package com.fahim.geminiapistarter;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Interaction.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract InteractionDao interactionDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            ctx.getApplicationContext(),
                            AppDatabase.class,
                            "chat.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}