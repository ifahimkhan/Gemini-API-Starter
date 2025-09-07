package com.fahim.geminiapistarter;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {UserQuery.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserQueryDao userQueryDao();
}
