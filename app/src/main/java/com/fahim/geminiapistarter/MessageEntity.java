package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String text;
    public boolean isUser;

    public MessageEntity(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }
}
