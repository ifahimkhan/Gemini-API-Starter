package com.fahim.geminiapistarter;

public class Message {
    public enum Sender { USER, BOT, TYPING }
    public final String text;
    public final Sender sender;
    public final long timestamp;

    public Message(String text, Sender sender, long ts) {
        this.text = text;
        this.sender = sender;
        this.timestamp = ts;
    }
}