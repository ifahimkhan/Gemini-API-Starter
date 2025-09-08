package com.fahim.C006_geminiapistarter;



public class Message {
    public String text;
    public boolean isUser; // true = user, false = Gemini

    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }
}