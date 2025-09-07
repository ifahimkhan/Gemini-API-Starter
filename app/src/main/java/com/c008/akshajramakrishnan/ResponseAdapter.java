package com.c008.akshajramakrishnan;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ResponseViewHolder> {

    private final List<Pair<String, SpannableStringBuilder>> responses;
    private final LayoutInflater inflater;

    public ResponseAdapter(Context context, List<Pair<String, SpannableStringBuilder>> responses) {
        this.inflater = LayoutInflater.from(context);
        this.responses = responses;
    }

    @NonNull
    @Override
    public ResponseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.response_item, parent, false);
        return new ResponseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResponseViewHolder holder, int position) {
        Pair<String, SpannableStringBuilder> pair = responses.get(position);
        holder.promptText.setText(pair.first);
        holder.responseText.setText(pair.second);
    }

    @Override
    public int getItemCount() {
        return responses.size();
    }

    public static class ResponseViewHolder extends RecyclerView.ViewHolder {
        TextView promptText, responseText;

        public ResponseViewHolder(@NonNull View itemView) {
            super(itemView);
            promptText = itemView.findViewById(R.id.promptText);
            responseText = itemView.findViewById(R.id.responseText);
        }
    }
}