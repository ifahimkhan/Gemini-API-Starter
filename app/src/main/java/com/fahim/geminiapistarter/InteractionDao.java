package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface InteractionDao {
    @Insert void insert(Interaction i);

    @Query("DELETE FROM Interaction")
    void clear();

    @Query("SELECT * FROM Interaction ORDER BY timestamp DESC LIMIT :limit")
    List<Interaction> latest(int limit);
}
