package com.fahim.geminiapistarter;

public class Message {
    private String text;
    private String sender;
    private long timestamp;

    public Message(String text, String sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }

    public void setText(String text) { this.text = text; }
    public void setSender(String sender) { this.sender = sender; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
