package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Interaction {
    @PrimaryKey(autoGenerate = true) public long id;
    public long timestamp;
    public String prompt;
    public String response;
}
