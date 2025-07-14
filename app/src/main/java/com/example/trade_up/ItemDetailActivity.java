package com.example.trade_up;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class ItemDetailActivity extends AppCompatActivity {
    private ImageView imgItemPhoto, imgSellerAvatar;
    private TextView tvItemTitle, tvItemPrice, tvItemDescription, tvItemCategory, tvItemCondition;
    private TextView tvSellerName, tvSellerRating;
    private TextView tvItemViews, tvItemInteractions;
    private LinearLayout layoutSellerInfo;
    private FirebaseFirestore db;
    private String sellerUid;
    private String itemId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        imgItemPhoto = findViewById(R.id.imgItemPhoto);
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvItemPrice = findViewById(R.id.tvItemPrice);
        tvItemDescription = findViewById(R.id.tvItemDescription);
        tvItemCategory = findViewById(R.id.tvItemCategory);
        tvItemCondition = findViewById(R.id.tvItemCondition);
        imgSellerAvatar = findViewById(R.id.imgSellerAvatar);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerRating = findViewById(R.id.tvSellerRating);
        tvItemViews = findViewById(R.id.tvItemViews);
        tvItemInteractions = findViewById(R.id.tvItemInteractions);
        layoutSellerInfo = findViewById(R.id.layoutSellerInfo);
        db = FirebaseFirestore.getInstance();

        // For demonstration, get item and seller info from intent extras
        itemId = getIntent().getStringExtra("ITEM_ID");
        sellerUid = getIntent().getStringExtra("SELLER_UID");
        incrementViewCount(itemId);
        loadItemDetails(itemId);
        loadSellerInfo(sellerUid);

        layoutSellerInfo.setOnClickListener(v -> {
            if (sellerUid != null && !sellerUid.isEmpty()) {
                Intent intent = new Intent(ItemDetailActivity.this, PublicProfileActivity.class);
                intent.putExtra("USER_UID", sellerUid);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Seller info not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadItemDetails(String itemId) {
        // Example: Load item details from Firestore (customize as needed)
        if (itemId == null) return;
        db.collection("items").document(itemId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    String category = documentSnapshot.getString("category");
                    String condition = documentSnapshot.getString("condition");
                    Double price = documentSnapshot.getDouble("price");
                    String photoUrl = documentSnapshot.getString("photoUrl");
                    int views = documentSnapshot.getLong("views") != null ? documentSnapshot.getLong("views").intValue() : 0;
                    int interactions = documentSnapshot.getLong("interactions") != null ? documentSnapshot.getLong("interactions").intValue() : 0;
                    tvItemViews.setText("Views: " + views);
                    tvItemInteractions.setText("Interactions: " + interactions);
                    tvItemTitle.setText(title != null ? title : "");
                    tvItemDescription.setText(description != null ? description : "");
                    tvItemCategory.setText(category != null ? category : "");
                    tvItemCondition.setText(condition != null ? condition : "");
                    tvItemPrice.setText(price != null ? "$" + String.format("%.2f", price) : "");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Picasso.get().load(photoUrl).placeholder(R.drawable.ic_launcher_background).into(imgItemPhoto);
                    } else {
                        imgItemPhoto.setImageResource(R.drawable.ic_launcher_background);
                    }
                }
            });
    }

    private void loadSellerInfo(String uid) {
        if (uid == null) return;
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayName = documentSnapshot.getString("displayName");
                    Double rating = documentSnapshot.getDouble("rating");
                    String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                    tvSellerName.setText(displayName != null ? displayName : "");
                    tvSellerRating.setText("Rating: " + (rating != null ? String.format("%.1f", rating) : "0.0"));
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get().load(profilePicUrl).placeholder(R.drawable.ic_person).into(imgSellerAvatar);
                    } else {
                        imgSellerAvatar.setImageResource(R.drawable.ic_person);
                    }
                }
            });
    }

    private void incrementViewCount(String itemId) {
        if (itemId == null) return;
        db.collection("items").document(itemId)
            .update("views", com.google.firebase.firestore.FieldValue.increment(1));
    }
}
