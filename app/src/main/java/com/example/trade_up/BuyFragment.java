package com.example.trade_up;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BuyFragment extends Fragment {
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> items = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buy, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ItemAdapter(requireContext(), items);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        loadItemsFromFirestore();
        return view;
    }

    private void loadItemsFromFirestore() {
        db.collection("items").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                items.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String id = doc.getId();
                    String title = doc.getString("title");
                    String description = doc.getString("description");
                    List<String> photoUrls = (List<String>) doc.get("photoUrls");
                    String imageUrl = (photoUrls != null && !photoUrls.isEmpty()) ? photoUrls.get(0) : "";
                    String sellerId = doc.getString("sellerUid");
                    double price = doc.getDouble("price") != null ? doc.getDouble("price") : 0.0;
                    items.add(new Item(id, title, description, imageUrl, sellerId, price));
                }
                adapter.notifyDataSetChanged();
            });
    }
}
