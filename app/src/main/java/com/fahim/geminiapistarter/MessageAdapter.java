package com.fahim.geminiapistarter;

import static android.view.View.TEXT_ALIGNMENT_TEXT_END;
import static android.view.View.TEXT_ALIGNMENT_TEXT_START;

import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
    private final List<Pair<String,String>> messages;

    public MessageAdapter(List<Pair<String,String>> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.message,parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Pair<String, String> message = messages.get(position);
        holder.message.setText(message.first);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = message.second.equals("user") ? Gravity.END : Gravity.START;
        holder.message.setLayoutParams(params);

    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
