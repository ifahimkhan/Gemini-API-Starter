

    package com.fahim.geminiapistarter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

    public class ResponseAdapter extends RecyclerView.Adapter<ResponseAdapter.ResponseViewHolder> {

        private List<String> responseList;

        public ResponseAdapter(List<String> responseList) {
            this.responseList = responseList;
        }

        @NonNull
        @Override
        public ResponseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ResponseViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResponseViewHolder holder, int position) {
            holder.responseText.setText(responseList.get(position));
        }

        @Override
        public int getItemCount() {
            return responseList.size();
        }

        static class ResponseViewHolder extends RecyclerView.ViewHolder {
            TextView responseText;

            ResponseViewHolder(@NonNull View itemView) {
                super(itemView);
                responseText = itemView.findViewById(android.R.id.text1);
            }
        }
    }


