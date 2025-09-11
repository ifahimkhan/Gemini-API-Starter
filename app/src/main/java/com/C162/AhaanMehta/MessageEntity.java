package com.C162.AhaanMehta;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public boolean isUser;
    public long timestamp;
}
