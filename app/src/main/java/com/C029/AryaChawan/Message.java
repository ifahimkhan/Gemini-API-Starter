package com.C029.AryaChawan;

public class Message {
    public String text;
    public boolean isUser; // true = user, false = Gemini

    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }
}
