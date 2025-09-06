package com.fahim.geminiapistarter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Message> list;
    private final Context context;

    public MessageAdapter(List<Message> list, Context context) {
        this.list = list;
        this.context = context;
    }

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_ai, parent, false);
            return new AiVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = list.get(position);
        String clean = m.getText()
                .replace("**", "<b>")
                .replace("*", "<i>")
                .replace("</i><i>", "");

        if (holder instanceof UserVH) {
            ((UserVH) holder).tv.setText(HtmlCompat.fromHtml(clean, HtmlCompat.FROM_HTML_MODE_LEGACY));
        } else if (holder instanceof AiVH) {
            ((AiVH) holder).tv.setText(HtmlCompat.fromHtml(clean, HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {
        TextView tv;
        UserVH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.messageTextView);
        }
    }

    static class AiVH extends RecyclerView.ViewHolder {
        TextView tv;
        AiVH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.messageTextView);
        }
    }
}