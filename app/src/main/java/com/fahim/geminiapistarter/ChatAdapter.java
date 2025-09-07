package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<ChatMessage> messages = new ArrayList<>();

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageText;
        private TextView timestampText;
        private TextView userMessageText;
        private TextView userTimestampText;
        private MaterialCardView aiMessageCard;
        private MaterialCardView userMessageCard;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
            userMessageText = itemView.findViewById(R.id.userMessageText);
            userTimestampText = itemView.findViewById(R.id.userTimestampText);
            aiMessageCard = itemView.findViewById(R.id.aiMessageCard);
            userMessageCard = itemView.findViewById(R.id.userMessageCard);
        }

        public void bind(ChatMessage message) {
            if (message.isUser()) {
                // Show user message
                userMessageCard.setVisibility(View.VISIBLE);
                aiMessageCard.setVisibility(View.GONE);
                userMessageText.setText(message.getMessage());
                userTimestampText.setText(android.text.format.DateFormat.format("HH:mm", message.getTimestamp()));
            } else {
                // Show AI message
                aiMessageCard.setVisibility(View.VISIBLE);
                userMessageCard.setVisibility(View.GONE);
                messageText.setText(message.getMessage());
                timestampText.setText(android.text.format.DateFormat.format("HH:mm", message.getTimestamp()));
            }
        }
    }
}
