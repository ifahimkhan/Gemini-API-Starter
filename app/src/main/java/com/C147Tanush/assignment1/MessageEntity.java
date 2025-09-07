package com.C147Tanush.assignment1;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String messageText;
    public long timestamp;

    public MessageEntity(String messageText, long timestamp) {
        this.messageText = messageText;
        this.timestamp = timestamp;
    }
}
