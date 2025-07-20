package com.example.trade_up;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
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
}
