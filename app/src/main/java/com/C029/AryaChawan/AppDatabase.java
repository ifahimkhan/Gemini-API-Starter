package com.C029.AryaChawan;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
}
