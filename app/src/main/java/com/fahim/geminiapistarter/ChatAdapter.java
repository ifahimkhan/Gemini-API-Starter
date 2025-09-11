package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class ChatAdapter extends ListAdapter<ChatMessage, ChatAdapter.ChatViewHolder> {

    protected ChatAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getTimestamp() == newItem.getTimestamp()
                            && oldItem.getMessage().equals(newItem.getMessage());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getMessage().equals(newItem.getMessage())
                            && oldItem.isUser() == newItem.isUser();
                }
            };

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.item_user_message : R.layout.item_ai_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        holder.messageTextView.setText(TextFormatter.getBoldSpannableText(message.getMessage()));
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isUser() ? 0 : 1;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
