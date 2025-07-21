package com.example.trade_up;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class OfferHistoryAdapter extends RecyclerView.Adapter<OfferHistoryAdapter.ViewHolder> {
    public OfferHistoryAdapter(java.util.List<Offer> list) {}
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
