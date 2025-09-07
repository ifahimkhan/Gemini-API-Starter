package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserQueryDao {
    @Insert
    void insert(UserQuery query);

    @Query("SELECT * FROM UserQuery")
    List<UserQuery> getAllQueries();
}
