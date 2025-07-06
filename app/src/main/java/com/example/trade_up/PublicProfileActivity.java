package com.example.trade_up;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class PublicProfileActivity extends AppCompatActivity {
    private ImageView imgProfilePicture;
    private TextView tvDisplayName, tvBio, tvContactInfo, tvRating;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvBio = findViewById(R.id.tvBio);
        tvContactInfo = findViewById(R.id.tvContactInfo);
        tvRating = findViewById(R.id.tvRating);
        db = FirebaseFirestore.getInstance();

        String userId = getIntent().getStringExtra("USER_ID");
        if (userId != null) {
            loadUserProfile(userId);
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserProfile(String userId) {
        DocumentReference docRef = db.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String displayName = documentSnapshot.getString("displayName");
                String bio = documentSnapshot.getString("bio");
                String contactInfo = documentSnapshot.getString("contactInfo");
                String photoUrl = documentSnapshot.getString("photoUrl");
                Double rating = documentSnapshot.getDouble("rating");
                tvDisplayName.setText(displayName != null ? displayName : "");
                tvBio.setText(bio != null ? bio : "");
                tvContactInfo.setText(contactInfo != null ? contactInfo : "");
                tvRating.setText("Rating: " + (rating != null ? rating : 0));
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Picasso.get().load(photoUrl).into(imgProfilePicture);
                }
            } else {
                Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

