package com.fahim.geminiapistarter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ArrayList<Message> messages;
    private final Context context;
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    public MessagesAdapter(Context context) {
        this.context = context;
        this.messages = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void updateMessage(int index, Message newMessage) {
        if (index >= 0 && index < messages.size()) {
            messages.set(index, newMessage);
            notifyItemChanged(index);
        }
    }

    public void setMessages(List<Message> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void clearMessages() {
        messages.clear();
        notifyDataSetChanged();
    }


    @Override
    public int getItemViewType(int position) {
        Message m = messages.get(position);
        if ("user".equals(m.getSender())) return VIEW_TYPE_USER;
        return VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(m.getText());
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).tvMessage.setText(m.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageUser);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessageBot);
        }
    }
}
