package com.C046.NeeravReddy.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {ChatMessage.class}, version = 1)
public abstract class ChatDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
}

