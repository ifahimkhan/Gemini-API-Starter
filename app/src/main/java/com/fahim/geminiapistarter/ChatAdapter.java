package com.fahim.geminiapistarter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fahim.geminiapistarter.R;
import com.fahim.geminiapistarter.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatMessage> messages;
    private final int USER = 1;
    private final int GEMINI = 2;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? USER : GEMINI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_gemini, parent, false);
            return new GeminiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).userMessage.setText(msg.getMessage());
        } else if (holder instanceof GeminiViewHolder) {
            ((GeminiViewHolder) holder).geminiMessage.setText(msg.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            userMessage = itemView.findViewById(R.id.userMessage);
        }
    }

    static class GeminiViewHolder extends RecyclerView.ViewHolder {
        TextView geminiMessage;
        GeminiViewHolder(View itemView) {
            super(itemView);
            geminiMessage = itemView.findViewById(R.id.geminiMessage);
        }
    }
}
