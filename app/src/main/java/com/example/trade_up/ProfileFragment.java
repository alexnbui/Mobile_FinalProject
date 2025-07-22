package com.example.trade_up;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class ProfileFragment extends Fragment {
    private ImageView imgProfilePic;
    private TextView tvDisplayName, tvEmail, tvBio, tvContactInfo, tvRating;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        imgProfilePic = view.findViewById(R.id.imgProfilePic);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvBio = view.findViewById(R.id.tvBio);
        tvContactInfo = view.findViewById(R.id.tvContactInfo);
        tvRating = view.findViewById(R.id.tvRating);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        loadUserProfile();
        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> showProfile(documentSnapshot, user))
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void showProfile(DocumentSnapshot doc, FirebaseUser user) {
        tvDisplayName.setText(doc.getString("displayName"));
        tvEmail.setText(user.getEmail());
        tvBio.setText(doc.getString("bio"));
        tvContactInfo.setText(doc.getString("contactInfo"));
        tvRating.setText("Rating: " + doc.getDouble("rating"));
        String picUrl = doc.getString("profilePicUrl");
        if (picUrl != null && !picUrl.isEmpty()) {
            Picasso.get().load(picUrl).placeholder(R.drawable.ic_launcher_background).into(imgProfilePic);
        } else {
            imgProfilePic.setImageResource(R.drawable.ic_launcher_background);
        }
    }
}
