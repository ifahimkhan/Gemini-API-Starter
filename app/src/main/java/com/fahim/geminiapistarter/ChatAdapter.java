package com.fahim.geminiapistarter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private List<ChatMessage> chatList;
    private Context context;

    public ChatAdapter(List<ChatMessage> chatList, Context context) {
        this.chatList = chatList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(
                viewType == 0 ? R.layout.item_chat_user : R.layout.item_chat_ai,
                parent,
                false
        );
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage chatMessage = chatList.get(position);
        holder.messageTextView.setText(chatMessage.getMessage());
        holder.timestampTextView.setText(chatMessage.getTimestamp());

        Animation fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
        holder.itemView.startAnimation(fadeIn);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatList.get(position).isUser() ? 0 : 1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView, timestampTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}