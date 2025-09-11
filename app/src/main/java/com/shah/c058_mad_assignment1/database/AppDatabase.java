package com.shah.c058_mad_assignment1.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {MessageChat.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
}
