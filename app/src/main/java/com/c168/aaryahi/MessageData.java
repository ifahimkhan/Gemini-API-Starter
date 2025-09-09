package com.c168.aaryahi;

public class MessageData {
    public static final int VIEW_TYPE_USER = 1;
    public static final int VIEW_TYPE_GEMINI = 2;

    private final String text;
    private final int viewType;

    public MessageData(String text, int viewType) { // Renamed constructor
        this.text = text;
        this.viewType = viewType;
    }

    public String getText() {
        return text;
    }

    public int getViewType() {
        return viewType;
    }
}
