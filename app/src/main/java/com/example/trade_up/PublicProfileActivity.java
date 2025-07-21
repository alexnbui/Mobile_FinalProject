package com.example.trade_up;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class PublicProfileActivity extends AppCompatActivity {
    private ImageView imgPublicProfilePicture;
    private TextView tvPublicDisplayName, tvPublicBio, tvPublicContactInfo, tvPublicRating, tvPublicTotalTransactions;
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();
    private FirebaseFirestore db;
    private Button btnReportUser, btnBlockUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);
        imgPublicProfilePicture = findViewById(R.id.imgPublicProfilePicture);
        tvPublicDisplayName = findViewById(R.id.tvPublicDisplayName);
        tvPublicBio = findViewById(R.id.tvPublicBio);
        tvPublicContactInfo = findViewById(R.id.tvPublicContactInfo);
        tvPublicRating = findViewById(R.id.tvPublicRating);
        tvPublicTotalTransactions = findViewById(R.id.tvPublicTotalTransactions);
        rvReviews = findViewById(R.id.rvPublicReviews);
        rvReviews.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(reviewAdapter);
        db = FirebaseFirestore.getInstance();

        String userUid = getIntent().getStringExtra("USER_UID");
        if (userUid == null || userUid.isEmpty()) {
            Toast.makeText(this, "No user specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadPublicProfile(userUid);

        btnReportUser = findViewById(R.id.btnReportUser);
        btnBlockUser = findViewById(R.id.btnBlockUser);
        btnReportUser.setOnClickListener(v -> showReportDialog(userUid));
        btnBlockUser.setOnClickListener(v -> showBlockDialog(userUid));
    }

    private void loadPublicProfile(String uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayName = documentSnapshot.getString("displayName");
                    String bio = documentSnapshot.getString("bio");
                    String contactInfo = documentSnapshot.getString("contactInfo");
                    Double rating = documentSnapshot.getDouble("rating");
                    Long totalTransactions = documentSnapshot.getLong("totalTransactions");
                    String profilePicUrl = documentSnapshot.getString("profilePicUrl");

                    tvPublicDisplayName.setText(displayName != null ? displayName : "");
                    tvPublicBio.setText(bio != null ? bio : "");
                    tvPublicContactInfo.setText(contactInfo != null ? contactInfo : "");
                    tvPublicRating.setText("Rating: " + (rating != null ? String.format("%.1f", rating) : "0.0"));
                    tvPublicTotalTransactions.setText("Total Transactions: " + (totalTransactions != null ? totalTransactions : 0));

                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get().load(profilePicUrl).placeholder(R.drawable.ic_person).into(imgPublicProfilePicture);
                    } else {
                        imgPublicProfilePicture.setImageResource(R.drawable.ic_person);
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
                loadUserReviews(uid);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void loadUserReviews(String userUid) {
        db.collection("reviews").whereEqualTo("toUserUid", userUid).get()
            .addOnSuccessListener(querySnapshot -> {
                reviewList.clear();
                for (var doc : querySnapshot) {
                    reviewList.add(new Review(
                        doc.getString("fromUserUid"),
                        doc.getDouble("rating"),
                        doc.getString("review"),
                        doc.getLong("timestamp")
                    ));
                }
                reviewAdapter.notifyDataSetChanged();
            });
    }

    private void showReportDialog(String reportedUid) {
        final String[] reasons = {"Scam/Fraud", "Inappropriate Content", "Spam"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report User")
            .setItems(reasons, (dialog, which) -> {
                String reason = reasons[which];
                submitReport(reportedUid, reason);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void submitReport(String reportedUid, String reason) {
        String reporterUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("reports").add(new Report(reporterUid, reportedUid, "user", reason, System.currentTimeMillis()))
            .addOnSuccessListener(documentReference ->
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show());
    }

    private void showBlockDialog(String blockedUid) {
        new AlertDialog.Builder(this)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block this user?")
            .setPositiveButton("Block", (dialog, which) -> blockUser(blockedUid))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void blockUser(String blockedUid) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(currentUid)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(blockedUid))
            .addOnSuccessListener(aVoid ->
                Toast.makeText(this, "User blocked", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to block user", Toast.LENGTH_SHORT).show());
    }

    private static class Review {
        String fromUserUid, review;
        Double rating;
        Long timestamp;
        Review(String fromUserUid, Double rating, String review, Long timestamp) {
            this.fromUserUid = fromUserUid;
            this.rating = rating;
            this.review = review;
            this.timestamp = timestamp;
        }
    }

    private class ReviewAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        List<Review> reviews;
        ReviewAdapter(List<Review> reviews) { this.reviews = reviews; }
        @Override
        public ReviewViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ReviewViewHolder(v);
        }
        @Override
        public void onBindViewHolder(ReviewViewHolder holder, int position) {
            Review r = reviews.get(position);
            holder.tv1.setText("Rating: " + (r.rating != null ? r.rating : "") + " ★");
            holder.tv2.setText(r.review != null ? r.review : "");
        }
        @Override
        public int getItemCount() { return reviews.size(); }
        class ReviewViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView tv1, tv2;
            ReviewViewHolder(android.view.View itemView) {
                super(itemView);
                tv1 = itemView.findViewById(android.R.id.text1);
                tv2 = itemView.findViewById(android.R.id.text2);
            }
        }
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
