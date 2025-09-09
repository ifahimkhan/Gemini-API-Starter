package com.c168.aaryahi;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(entity = Conversation.class,
                parentColumns = "id",
                childColumns = "conversationId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("conversationId")})
public class MessageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int conversationId;

    public String text;

    public int viewType;

    public MessageEntity(int conversationId, String text, int viewType) {
        this.conversationId = conversationId;
        this.text = text;
        this.viewType = viewType;
    }
}
