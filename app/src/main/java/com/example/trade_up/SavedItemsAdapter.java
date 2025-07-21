package com.example.trade_up;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class SavedItemsAdapter extends RecyclerView.Adapter<SavedItemsAdapter.ViewHolder> {
    public SavedItemsAdapter(java.util.List<Item> list) {}
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {}
    @Override
    public int getItemCount() { return 0; }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) { super(itemView); }
    }
}
