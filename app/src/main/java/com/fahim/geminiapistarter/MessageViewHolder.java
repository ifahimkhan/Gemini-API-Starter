package com.fahim.geminiapistarter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    TextView message;
    @NonNull
    View root;
    public MessageViewHolder(@NonNull View itemView) {

        super(itemView);
        message = itemView.findViewById(R.id.message);
        root = itemView;
    }
}
