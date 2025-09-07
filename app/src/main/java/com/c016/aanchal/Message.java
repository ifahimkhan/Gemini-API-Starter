package com.c016.aanchal;

public class Message {
    private String text;
    private boolean isSentByMe;
    private boolean user;
    public Message(String text, boolean isSentByMe) {
        this.text = text;
        this.isSentByMe = isSentByMe;
    }

    public String getText() {
        return text;
    }

    public boolean isSentByMe() {
        return isSentByMe;
    }

    public boolean isUser() { return user; }


}
