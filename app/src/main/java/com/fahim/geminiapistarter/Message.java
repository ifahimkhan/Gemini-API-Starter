package com.fahim.geminiapistarter;

public class Message {
    public final long id;
    public final String text;
    public final boolean fromUser;

    public Message(long id, String text, boolean fromUser) {
        this.id = id;
        this.text = text;
        this.fromUser = fromUser;
    }
}
