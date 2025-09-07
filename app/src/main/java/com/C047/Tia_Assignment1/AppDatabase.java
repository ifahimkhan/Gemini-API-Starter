package com.C047.Tia_Assignment1;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
}
