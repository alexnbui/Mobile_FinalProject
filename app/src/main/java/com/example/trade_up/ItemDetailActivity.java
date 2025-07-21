package com.example.trade_up;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class ItemDetailActivity extends AppCompatActivity {
    private ImageView imgItemPhoto, imgSellerAvatar;
    private TextView tvItemTitle, tvItemPrice, tvItemDescription, tvItemCategory, tvItemCondition;
    private TextView tvSellerName, tvSellerRating;
    private TextView tvItemViews, tvItemInteractions;
    private LinearLayout layoutSellerInfo;
    private FirebaseFirestore db;
    private String sellerUid;
    private String itemId;
    private Button btnChat, btnMakeOffer, btnMarkSold, btnReportListing, btnBlockSeller;
    private FirebaseUser currentUser;
    private RecyclerView rvOffers;
    private OfferAdapter offerAdapter;
    private List<Offer> offerList = new ArrayList<>();
    private Button btnMockPayment;

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
        btnChat = findViewById(R.id.btnChat);
        btnMakeOffer = findViewById(R.id.btnMakeOffer);
        btnMarkSold = findViewById(R.id.btnMarkSold);
        rvOffers = findViewById(R.id.rvOffers);
        btnReportListing = findViewById(R.id.btnReportListing);
        btnBlockSeller = findViewById(R.id.btnBlockSeller);
        btnMockPayment = findViewById(R.id.btnMockPayment);
        rvOffers.setVisibility(View.GONE);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Get itemId from intent
        itemId = getIntent().getStringExtra("ITEM_ID");
        if (itemId == null) {
            Toast.makeText(this, "No item ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadItemDetails(itemId);
        btnMakeOffer.setOnClickListener(v -> showMakeOfferDialog());
        btnMarkSold.setOnClickListener(v -> markItemAsSold());
        btnReportListing.setOnClickListener(v -> showReportListingDialog());
        btnBlockSeller.setOnClickListener(v -> showBlockSellerDialog());
        btnMockPayment.setOnClickListener(v -> showMockPaymentDialog());
    }

    private void showMakeOfferDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter your offer price");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);
        builder.setPositiveButton("Send Offer", (dialog, which) -> {
            String offerStr = input.getText().toString().trim();
            if (!offerStr.isEmpty() && currentUser != null) {
                double offerPrice = Double.parseDouble(offerStr);
                saveOfferToFirestore(offerPrice);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveOfferToFirestore(double offerPrice) {
        if (itemId == null || sellerUid == null || currentUser == null) return;
        String offerId = db.collection("offers").document().getId();
        db.collection("offers").document(offerId).set(new java.util.HashMap<String, Object>() {{
            put("itemId", itemId);
            put("sellerUid", sellerUid);
            put("buyerUid", currentUser.getUid());
            put("offerPrice", offerPrice);
            put("status", "pending");
            put("timestamp", System.currentTimeMillis());
        }}).addOnSuccessListener(aVoid -> Toast.makeText(ItemDetailActivity.this, "Offer sent!", Toast.LENGTH_SHORT).show())
        .addOnFailureListener(e -> Toast.makeText(ItemDetailActivity.this, "Failed to send offer", Toast.LENGTH_SHORT).show());
    }

    private void markItemAsSold() {
        if (itemId == null) return;
        db.collection("items").document(itemId).update("status", "Sold")
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Item marked as Sold", Toast.LENGTH_SHORT).show();
                showRatingDialog();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to mark as Sold", Toast.LENGTH_SHORT).show());
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rate your transaction");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating_review, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etReview = dialogView.findViewById(R.id.etReview);
        builder.setView(dialogView);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            float rating = ratingBar.getRating();
            String review = etReview.getText().toString().trim();
            if (rating > 0 && sellerUid != null && currentUser != null) {
                submitRatingAndReview(sellerUid, currentUser.getUid(), rating, review);
            } else {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void submitRatingAndReview(String toUserUid, String fromUserUid, float rating, String review) {
        // Basic moderation: filter abusive words (simple example)
        String[] abusiveWords = {"badword1", "badword2"};
        for (String word : abusiveWords) {
            if (review.toLowerCase().contains(word)) {
                Toast.makeText(this, "Review contains inappropriate content.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String reviewId = db.collection("reviews").document().getId();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("toUserUid", toUserUid);
        data.put("fromUserUid", fromUserUid);
        data.put("rating", rating);
        data.put("review", review);
        data.put("timestamp", System.currentTimeMillis());
        db.collection("reviews").document(reviewId).set(data)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show());
        // Optionally, update user's average rating and transaction count
        updateUserRatingStats(toUserUid, rating);
    }

    private void updateUserRatingStats(String userUid, float newRating) {
        db.collection("users").document(userUid).get().addOnSuccessListener(doc -> {
            Double currentAvg = doc.getDouble("rating");
            Long total = doc.getLong("totalTransactions");
            double avg = currentAvg != null ? currentAvg : 0;
            long count = total != null ? total : 0;
            double newAvg = (avg * count + newRating) / (count + 1);
            db.collection("users").document(userUid).update("rating", newAvg, "totalTransactions", count + 1);
        });
    }

    private void loadItemDetails(String itemId) {
        db.collection("items").document(itemId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String title = documentSnapshot.getString("title");
                    String description = documentSnapshot.getString("description");
                    Double price = documentSnapshot.getDouble("price");
                    String category = documentSnapshot.getString("category");
                    String condition = documentSnapshot.getString("condition");
                    String sellerId = documentSnapshot.getString("sellerUid");
                    java.util.List<String> photoUrls = (java.util.List<String>) documentSnapshot.get("photoUrls");
                    tvItemTitle.setText(title != null ? title : "");
                    tvItemPrice.setText(price != null ? "$" + price : "");
                    tvItemDescription.setText(description != null ? description : "");
                    tvItemCategory.setText(category != null ? "Category: " + category : "");
                    tvItemCondition.setText(condition != null ? "Condition: " + condition : "");
                    if (photoUrls != null && !photoUrls.isEmpty()) {
                        Picasso.get().load(photoUrls.get(0)).into(imgItemPhoto);
                    } else {
                        imgItemPhoto.setImageResource(R.drawable.ic_launcher_background);
                    }
                    sellerUid = sellerId;
                    if (currentUser != null && currentUser.getUid().equals(sellerUid)) {
                        btnMakeOffer.setVisibility(Button.GONE);
                        btnMarkSold.setVisibility(Button.VISIBLE);
                        rvOffers.setVisibility(View.VISIBLE);
                        loadOffersForSeller();
                    } else {
                        btnMakeOffer.setVisibility(Button.VISIBLE);
                        btnMarkSold.setVisibility(Button.GONE);
                        rvOffers.setVisibility(View.GONE);
                    }
                    loadSellerInfo(sellerUid);
                } else {
                    Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load item", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void loadOffersForSeller() {
        if (itemId == null) return;
        db.collection("offers").whereEqualTo("itemId", itemId).get()
            .addOnSuccessListener(querySnapshot -> {
                offerList.clear();
                for (var doc : querySnapshot) {
                    offerList.add(new Offer(
                        doc.getId(),
                        doc.getString("buyerUid"),
                        doc.getDouble("offerPrice"),
                        doc.getString("status")
                    ));
                }
                if (offerAdapter == null) {
                    offerAdapter = new OfferAdapter(offerList);
                    rvOffers.setAdapter(offerAdapter);
                } else {
                    offerAdapter.notifyDataSetChanged();
                }
            });
    }

    private class Offer {
        String id, buyerUid, status;
        Double price;
        Offer(String id, String buyerUid, Double price, String status) {
            this.id = id;
            this.buyerUid = buyerUid;
            this.price = price;
            this.status = status;
        }
    }

    private class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {
        List<Offer> offers;
        OfferAdapter(List<Offer> offers) { this.offers = offers; }
        @Override
        public OfferViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new OfferViewHolder(v);
        }
        @Override
        public void onBindViewHolder(OfferViewHolder holder, int position) {
            Offer offer = offers.get(position);
            holder.tv1.setText("Buyer: " + offer.buyerUid);
            holder.tv2.setText("Offer: $" + offer.price + " | Status: " + offer.status);
            holder.itemView.setOnClickListener(v -> showOfferActionDialog(offer));
        }
        @Override
        public int getItemCount() { return offers.size(); }
        class OfferViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            OfferViewHolder(View itemView) {
                super(itemView);
                tv1 = itemView.findViewById(android.R.id.text1);
                tv2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    private void showOfferActionDialog(Offer offer) {
        String[] actions = {"Accept", "Reject", "Counter"};
        new AlertDialog.Builder(this)
            .setTitle("Offer from: " + offer.buyerUid)
            .setItems(actions, (dialog, which) -> {
                if (which == 0) acceptOffer(offer);
                else if (which == 1) rejectOffer(offer);
                else if (which == 2) showCounterOfferDialog(offer);
            })
            .show();
    }
    private void acceptOffer(Offer offer) {
        db.collection("offers").document(offer.id).update("status", "accepted");
        db.collection("items").document(itemId).update("status", "Sold");
        Toast.makeText(this, "Offer accepted, item marked as Sold", Toast.LENGTH_SHORT).show();
        loadOffersForSeller();
    }
    private void rejectOffer(Offer offer) {
        db.collection("offers").document(offer.id).update("status", "rejected");
        Toast.makeText(this, "Offer rejected", Toast.LENGTH_SHORT).show();
        loadOffersForSeller();
    }
    private void showCounterOfferDialog(Offer offer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Counter Offer Price");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);
        builder.setPositiveButton("Send", (dialog, which) -> {
            String priceStr = input.getText().toString().trim();
            if (!priceStr.isEmpty()) {
                double counterPrice = Double.parseDouble(priceStr);
                db.collection("offers").document(offer.id).update("status", "countered", "counterPrice", counterPrice);
                Toast.makeText(this, "Counter offer sent", Toast.LENGTH_SHORT).show();
                loadOffersForSeller();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadSellerInfo(String sellerUid) {
        if (sellerUid == null) return;
        db.collection("users").document(sellerUid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayName = documentSnapshot.getString("displayName");
                    Double rating = documentSnapshot.getDouble("rating");
                    String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                    tvSellerName.setText(displayName != null ? displayName : "");
                    tvSellerRating.setText(rating != null ? String.format("%.1f ★", rating) : "");
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get().load(profilePicUrl).into(imgSellerAvatar);
                    } else {
                        imgSellerAvatar.setImageResource(R.drawable.ic_person);
                    }
                }
            });
        layoutSellerInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, PublicProfileActivity.class);
            intent.putExtra("SELLER_UID", sellerUid);
            startActivity(intent);
        });
        btnChat.setOnClickListener(v -> {
            if (sellerUid != null && !sellerUid.isEmpty()) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("USER_ID", sellerUid);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Seller info not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void incrementViewCount(String itemId) {
        if (itemId == null) return;
        db.collection("items").document(itemId)
            .update("views", com.google.firebase.firestore.FieldValue.increment(1));
    }

    private void showReportListingDialog() {
        final String[] reasons = {"Scam/Fraud", "Inappropriate Content", "Spam"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Listing")
            .setItems(reasons, (dialog, which) -> {
                String reason = reasons[which];
                submitListingReport(reason);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void submitListingReport(String reason) {
        String reporterUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("reports").add(new Report(reporterUid, itemId, "listing", reason, System.currentTimeMillis()))
            .addOnSuccessListener(documentReference ->
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show());
    }

    private void showBlockSellerDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Block Seller")
            .setMessage("Are you sure you want to block this seller?")
            .setPositiveButton("Block", (dialog, which) -> blockSeller())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void blockSeller() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(currentUid)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(sellerUid))
            .addOnSuccessListener(aVoid ->
                Toast.makeText(this, "Seller blocked", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to block seller", Toast.LENGTH_SHORT).show());
    }

    private void showMockPaymentDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Mock Payment")
            .setMessage("Confirm payment for this item?")
            .setPositiveButton("Pay", (dialog, which) -> performMockPayment())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performMockPayment() {
        String userUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        String paymentId = db.collection("payments").document().getId();
        double amount = 0;
        if (tvItemPrice.getText() != null) {
            try {
                amount = Double.parseDouble(tvItemPrice.getText().toString().replace("$", ""));
            } catch (Exception ignored) {}
        }
        java.util.Map<String, Object> payment = new java.util.HashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("userUid", userUid);
        payment.put("itemId", itemId);
        payment.put("amount", amount);
        payment.put("method", "Mock");
        payment.put("timestamp", System.currentTimeMillis());
        db.collection("payments").document(paymentId).set(payment)
            .addOnSuccessListener(aVoid ->
                Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Payment failed!", Toast.LENGTH_SHORT).show());
    }

    // Helper class for report
    class Report {
        public String reporterId, targetId, type, reason;
        public long timestamp;
        public Report(String reporterId, String targetId, String type, String reason, long timestamp) {
            this.reporterId = reporterId;
            this.targetId = targetId;
            this.type = type;
            this.reason = reason;
            this.timestamp = timestamp;
        }
    }
}
