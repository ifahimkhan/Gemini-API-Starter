package com.example.c077_assignment1;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        View messageContainer;

        ViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getMessage());

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) messageContainer.getLayoutParams();

            if (message.isUser()) {
                params.gravity = Gravity.END;
                messageContainer.setBackgroundResource(R.drawable.user_message_bg);
            } else {
                params.gravity = Gravity.START;
                messageContainer.setBackgroundResource(R.drawable.bot_message_bg);
            }
            messageContainer.setLayoutParams(params);
        }
    }
}
