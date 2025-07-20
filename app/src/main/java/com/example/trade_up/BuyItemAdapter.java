package com.example.trade_up;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BuyItemAdapter extends RecyclerView.Adapter<BuyItemAdapter.BuyItemViewHolder> {
    private List<String> items; // Replace String with your actual item model
    private Context context;

    public BuyItemAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public BuyItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new BuyItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuyItemViewHolder holder, int position) {
        holder.textView.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class BuyItemViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        public BuyItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

