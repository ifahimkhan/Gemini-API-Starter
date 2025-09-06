package com.fahim.geminiapistarter;

import android.text.SpannableStringBuilder;

public class ChatMessage {
    public final SpannableStringBuilder query;
    public final SpannableStringBuilder response;

    public ChatMessage(SpannableStringBuilder query, SpannableStringBuilder response) {
        this.query = query;
        this.response = response;
    }
}
