package com.c008.akshajramakrishnan;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatDao {
    @Insert
    void insert(Chat chat);

    @Query("SELECT * FROM chat_table ORDER BY id ASC")
    List<Chat> getAllChats();

    @Query("DELETE FROM chat_table")
    void deleteAll();

    @Query("DELETE FROM chat_table")
    void deleteAllChats();
}
