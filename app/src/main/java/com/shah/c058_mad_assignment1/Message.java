package com.shah.c058_mad_assignment1;

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
