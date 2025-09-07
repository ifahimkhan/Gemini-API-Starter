package com.c178.parthacharya;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;
    private final LayoutInflater inflater;

    public ResponseAdapter(Context context, List<ChatMessage> messages) {
        this.inflater = LayoutInflater.from(context);
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.queryTextView.setText(message.userMessage);
        holder.responseTextView.setText(message.aiResponse);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        final TextView queryTextView;
        final TextView responseTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            queryTextView = itemView.findViewById(R.id.queryTextView);
            responseTextView = itemView.findViewById(R.id.responseTextView);
        }
    }
}
