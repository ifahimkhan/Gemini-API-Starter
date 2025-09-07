package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationHistoryAdapter extends RecyclerView.Adapter<ConversationHistoryAdapter.ConversationViewHolder> {
    private List<ConversationSession> conversations = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ConversationSession conversation);
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation_history, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationSession conversation = conversations.get(position);
        holder.bind(conversation);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public void setConversations(List<ConversationSession> conversations) {
        this.conversations = conversations;
        notifyDataSetChanged();
    }

    public void addConversation(ConversationSession conversation) {
        this.conversations.add(0, conversation);
        notifyItemInserted(0);
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        private TextView titleText;
        private TextView previewText;
        private TextView timeText;
        private TextView messageCountText;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.conversationTitle);
            previewText = itemView.findViewById(R.id.conversationPreview);
            timeText = itemView.findViewById(R.id.conversationTime);
            messageCountText = itemView.findViewById(R.id.messageCount);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onConversationClick(conversations.get(position));
                    }
                }
            });
        }

        public void bind(ConversationSession conversation) {
            titleText.setText(conversation.getSessionTitle());
            
            // Show first user message as preview
            String preview = conversation.getFirstUserMessage();
            if (preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            previewText.setText(preview);
            
            // Format time
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            timeText.setText(sdf.format(new Date(conversation.getStartTime())));
            
            // Show message count
            messageCountText.setText(conversation.getMessageCount() + " messages");
        }
    }
}
