package com.example.trade_up;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class SellFragment extends Fragment {
    private RecyclerView recyclerViewListings;
    private FloatingActionButton fabAddItem;
    private ListingAdapter adapter;
    private List<Listing> listingList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sell, container, false);
        recyclerViewListings = view.findViewById(R.id.recyclerViewListings);
        fabAddItem = view.findViewById(R.id.fabAddItem);
        recyclerViewListings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ListingAdapter(listingList);
        recyclerViewListings.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        fabAddItem.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddItemActivity.class));
        });
        loadUserListings();
        return view;
    }

    private void loadUserListings() {
        if (currentUid == null) return;
        db.collection("items").whereEqualTo("sellerUid", currentUid).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                listingList.clear();
                for (var doc : queryDocumentSnapshots) {
                    String id = doc.getId();
                    String title = doc.getString("title");
                    String photoUrl = doc.getString("photoUrl");
                    String status = doc.getString("status");
                    int views = doc.getLong("views") != null ? doc.getLong("views").intValue() : 0;
                    int interactions = doc.getLong("interactions") != null ? doc.getLong("interactions").intValue() : 0;
                    listingList.add(new Listing(id, title, photoUrl, status, views, interactions));
                }
                adapter.notifyDataSetChanged();
            });
    }

    private static class Listing {
        String id, title, photoUrl, status;
        int views, interactions;
        Listing(String id, String title, String photoUrl, String status, int views, int interactions) {
            this.id = id;
            this.title = title;
            this.photoUrl = photoUrl;
            this.status = status;
            this.views = views;
            this.interactions = interactions;
        }
    }

    private class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {
        private final List<Listing> listings;
        ListingAdapter(List<Listing> listings) { this.listings = listings; }
        @NonNull
        @Override
        public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listing_item, parent, false);
            return new ListingViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
            Listing listing = listings.get(position);
            holder.tvTitle.setText(listing.title);
            holder.tvStatus.setText(listing.status);
            holder.tvViews.setText("Views: " + listing.views);
            holder.tvInteractions.setText("Interactions: " + listing.interactions);
            if (listing.photoUrl != null && !listing.photoUrl.isEmpty()) {
                com.squareup.picasso.Picasso.get().load(listing.photoUrl).placeholder(R.drawable.ic_launcher_background).into(holder.imgPhoto);
            } else {
                holder.imgPhoto.setImageResource(R.drawable.ic_launcher_background);
            }
            holder.btnEdit.setOnClickListener(v -> editListing(listing));
            holder.btnDelete.setOnClickListener(v -> deleteListing(listing));
        }
        @Override
        public int getItemCount() { return listings.size(); }
        class ListingViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPhoto;
            TextView tvTitle, tvStatus, tvViews, tvInteractions;
            Button btnEdit, btnDelete;
            ListingViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.imgListingPhoto);
                tvTitle = itemView.findViewById(R.id.tvListingTitle);
                tvStatus = itemView.findViewById(R.id.tvListingStatus);
                tvViews = itemView.findViewById(R.id.tvListingViews);
                tvInteractions = itemView.findViewById(R.id.tvListingInteractions);
                btnEdit = itemView.findViewById(R.id.btnEditListing);
                btnDelete = itemView.findViewById(R.id.btnDeleteListing);
            }
        }
    }

    private void editListing(Listing listing) {
        Intent intent = new Intent(getActivity(), AddItemActivity.class);
        intent.putExtra("EDIT_MODE", true);
        intent.putExtra("ITEM_ID", listing.id);
        startActivity(intent);
    }

    private void deleteListing(Listing listing) {
        db.collection("items").document(listing.id).delete()
            .addOnSuccessListener(aVoid -> loadUserListings());
    }
}
