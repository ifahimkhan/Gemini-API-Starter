package com.C162.AhaanMehta;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_response, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (msg.isUser()) {
            // Show user message in the top textview
            holder.itemPromptTextView.setText(msg.getMessage());
            holder.itemResponseTextView.setText(""); // clear AI response
        } else {
            // Show AI response in the bottom textview
            holder.itemPromptTextView.setText("");   // no user text
            holder.itemResponseTextView.setText(msg.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView itemPromptTextView;
        TextView itemResponseTextView;

        ChatViewHolder(View itemView) {
            super(itemView);
            itemPromptTextView = itemView.findViewById(R.id.itemPromptTextView);
            itemResponseTextView = itemView.findViewById(R.id.itemResponseTextView);
        }
    }
}
