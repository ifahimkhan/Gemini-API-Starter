package com.fahim.geminiapistarter;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    private int messageType;
    private String messageContent;
    private long timestamp;

    public ChatMessage(int messageType, String messageContent) {
        this.messageType = messageType;
        this.messageContent = messageContent;
        this.timestamp = System.currentTimeMillis();
    }

    // Add getters
    public int getMessageType() { return messageType; }
    public String getMessageContent() { return messageContent; }
    public long getTimestamp() { return timestamp; }
}