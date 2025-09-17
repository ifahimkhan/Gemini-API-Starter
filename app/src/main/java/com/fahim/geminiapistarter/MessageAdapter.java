package com.fahim.geminiapistarter;

import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;
    private static final int TYPE_TYPING = 3;

    private final List<Message> data = new ArrayList<>();

    public void submit(Message m) {
        data.add(m);
        notifyItemInserted(data.size() - 1);
    }

    public void replaceTypingWith(Message m) {
        int last = data.size() - 1;
        if (last >= 0 && data.get(last).sender == Message.Sender.TYPING) {
            data.set(last, m);
            notifyItemChanged(last);
        } else {
            submit(m);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message.Sender s = data.get(position).sender;
        if (s == Message.Sender.USER) return TYPE_USER;
        if (s == Message.Sender.TYPING) return TYPE_TYPING;
        return TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_USER) ? R.layout.item_msg_user : R.layout.item_msg_bot;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VH vh = (VH) holder;
        Message msg = data.get(position);
        if (msg.sender == Message.Sender.TYPING) {
            vh.tv.setText("…");
        } else if (msg.sender == Message.Sender.BOT) {
            SpannableStringBuilder bolded = TextFormatter.getBoldSpannableText(msg.text);
            vh.tv.setText(bolded);
        } else {
            vh.tv.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;

        VH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.tvText);
        }
    }
}