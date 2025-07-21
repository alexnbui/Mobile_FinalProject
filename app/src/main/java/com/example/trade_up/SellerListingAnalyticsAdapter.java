package com.example.trade_up;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

public class SellerListingAnalyticsAdapter extends RecyclerView.Adapter<SellerListingAnalyticsAdapter.ViewHolder> {
    public SellerListingAnalyticsAdapter(List<SellerListingAnalytics> list) {}

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Stub implementation
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
