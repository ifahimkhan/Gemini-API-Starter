package com.fahim.geminiapistarter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fahim.geminiapistarter.ui.theme.UiTheme;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER_MESSAGE = 1;
    private static final int VIEW_TYPE_API_MESSAGE = 2;

    private List<ChatMessage> chatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        if (message.getMessageType() == ChatMessage.MessageType.USER) {
            return VIEW_TYPE_USER_MESSAGE;
        } else {
            return VIEW_TYPE_API_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER_MESSAGE) {
            View view = inflater.inflate(R.layout.item_user_message, parent, false);
            return new UserMessageViewHolder(view);
        } else { // VIEW_TYPE_API_MESSAGE
            View view = inflater.inflate(R.layout.item_api_message, parent, false);
            return new ApiMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_USER_MESSAGE) {
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            ((ApiMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages == null ? 0 : chatMessages.size();
    }

    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView userMessageTextView;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            userMessageTextView = itemView.findViewById(R.id.bubble_outgoing);
        }

        void bind(ChatMessage message) {
            userMessageTextView.setText(message.getMessage());
            Context context = itemView.getContext();
            Integer accentColor = UiTheme.INSTANCE.loadAccent(context);

            if (accentColor != null) {
                Drawable background = AppCompatResources.getDrawable(context, R.drawable.bg_bubble_outgoing);
                if (background != null) {
                    Drawable mutableBackground = DrawableCompat.wrap(background).mutate();
                    DrawableCompat.setTint(mutableBackground, accentColor);
                    userMessageTextView.setBackground(mutableBackground);
                    userMessageTextView.setTextColor(UiTheme.INSTANCE.bestOnColor(accentColor));
                }
            } else {
                // Optional: Set a default background tint and text color if no accent is chosen
                // For now, it will use the drawable as is (no tint) and default text color from XML
            }
        }
    }

    static class ApiMessageViewHolder extends RecyclerView.ViewHolder {
        TextView apiMessageTextView;

        ApiMessageViewHolder(View itemView) {
            super(itemView);
            apiMessageTextView = itemView.findViewById(R.id.bubble_incoming);
        }

        void bind(ChatMessage message) {
            apiMessageTextView.setText(message.getMessage());
            // Ensure text color is appropriate for the neutral background
            // The XML already sets it to @android:color/black, which is usually fine.
            // apiMessageTextView.setTextColor(Color.BLACK); // Or a color from your theme attrs
        }
    }

    public void addMessage(ChatMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
    }
}
