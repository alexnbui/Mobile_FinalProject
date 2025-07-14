package com.example.trade_up;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class PublicProfileActivity extends AppCompatActivity {
    private ImageView imgPublicProfilePicture;
    private TextView tvPublicDisplayName, tvPublicBio, tvPublicContactInfo, tvPublicRating, tvPublicTotalTransactions;
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
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                finish();
            });
    }
}

