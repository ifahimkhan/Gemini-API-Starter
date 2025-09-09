package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> items = new ArrayList<>();
    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSIST = 2;

    @Override
    public int getItemViewType(int position) {
        return items.get(position).fromUser ? TYPE_USER : TYPE_ASSIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == TYPE_USER) {
            view = inf.inflate(R.layout.item_message_user, parent, false);
            return new SimpleVH(view);
        } else {
            view = inf.inflate(R.layout.item_message_assistant, parent, false);
            return new SimpleVH(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TextView tv = holder.itemView.findViewById(R.id.messageText);
        tv.setText(items.get(position).text);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addMessage(Message msg) {
        items.add(msg);
        notifyItemInserted(items.size() - 1);
    }

    public void submit(List<Message> messages) {
        items.clear();
        items.addAll(messages);
        notifyDataSetChanged();
    }

    static class SimpleVH extends RecyclerView.ViewHolder {
        SimpleVH(@NonNull View itemView) {
            super(itemView);
        }
    }
}
