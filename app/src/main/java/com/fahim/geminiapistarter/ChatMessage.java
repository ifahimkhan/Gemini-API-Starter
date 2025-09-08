package com.fahim.geminiapistarter;

public class ChatMessage {
    public enum MessageType {
        USER,
        API
    }

    private String message;
    private MessageType messageType;

    public ChatMessage(String message, MessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}