package com.shah.c058_mad_assignment1.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {
    @Insert
    void insert(MessageChat message);

    @Query("SELECT * FROM messages")
    List<MessageChat> getAllMessages();

    @Query("DELETE FROM messages")
    void deleteAll();
}