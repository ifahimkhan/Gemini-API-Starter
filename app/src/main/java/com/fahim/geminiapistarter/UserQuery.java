package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class UserQuery {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String query;

    public UserQuery(String query) {
        this.query = query;
    }

    public UserQuery() {

    }
}
