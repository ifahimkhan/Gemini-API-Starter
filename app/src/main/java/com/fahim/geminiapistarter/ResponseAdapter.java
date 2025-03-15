package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ResponseViewHolder> {

    private final ArrayList<String> responseList;

    public ResponseAdapter(ArrayList<String> responseList) {
        this.responseList = responseList;
    }

    @NonNull
    @Override
    public ResponseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a simple layout for each row in the RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_response, parent, false);
        return new ResponseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResponseViewHolder holder, int position) {
        String responseText = responseList.get(position);
        holder.bind(responseText);
    }

    @Override
    public int getItemCount() {
        return responseList.size();
    }

    // Helper method to add a new response to the list and update UI
    public void addResponse(String newResponse) {
        responseList.add(newResponse);
        notifyItemInserted(responseList.size() - 1);
    }

    // ViewHolder represents each item (row) in the RecyclerView
    static class ResponseViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ResponseViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.responseTextView);
        }

        void bind(String response) {
            // Use the bold text formatter if needed
            textView.setText(TextFormatter.getBoldSpannableText(response));
        }
    }
}
