package com.example.trade_up;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgProfilePicture;
    private Button btnChangePhoto, btnSaveProfile, btnDeleteAccount, btnLogout, btnDeactivateAccount;
    private TextInputEditText etDisplayName, etBio, etContactInfo;
    private TextView tvRating;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private String photoUrl = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        addControls();
        addEvents();
        loadUserProfile();
    }

    private void addControls() {
        imgProfilePicture = findViewById(R.id.imgProfilePicture);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeactivateAccount = findViewById(R.id.btnDeactivateAccount);
        etDisplayName = findViewById(R.id.etDisplayName);
        etBio = findViewById(R.id.etBio);
        etContactInfo = findViewById(R.id.etContactInfo);
        tvRating = findViewById(R.id.tvRating);
    }

    private void addEvents() {
        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });
        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserProfile();
            }
        });
        btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteAccountDialog();
            }
        });
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        btnDeactivateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeactivateAccountDialog();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                imgProfilePicture.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Load profile picture
        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayName = documentSnapshot.getString("displayName");
                    String bio = documentSnapshot.getString("bio");
                    String contactInfo = documentSnapshot.getString("contactInfo");
                    Double rating = documentSnapshot.getDouble("rating");
                    String profilePicUrl = documentSnapshot.getString("profilePicUrl");

                    etDisplayName.setText(displayName != null ? displayName : "");
                    etBio.setText(bio != null ? bio : "");
                    etContactInfo.setText(contactInfo != null ? contactInfo : "");
                    tvRating.setText("Rating: " + (rating != null ? String.format("%.1f", rating) : "0.0"));

                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get().load(profilePicUrl).placeholder(R.drawable.ic_person).into(imgProfilePicture);
                    } else {
                        imgProfilePicture.setImageResource(R.drawable.ic_person);
                    }
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        final String uid = user.getUid();
        final String displayName = etDisplayName.getText() != null ? etDisplayName.getText().toString().trim() : "";
        final String bio = etBio.getText() != null ? etBio.getText().toString().trim() : "";
        final String contactInfo = etContactInfo.getText() != null ? etContactInfo.getText().toString().trim() : "";
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Display name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageUri != null) {
            final StorageReference fileRef = storageRef.child(uid + ".jpg");
            fileRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    photoUrl = uri.toString();
                                    saveProfileToFirestore(uid, displayName, bio, contactInfo, photoUrl);
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(UserProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            saveProfileToFirestore(uid, displayName, bio, contactInfo, photoUrl);
        }
    }

    private void saveProfileToFirestore(String uid, String displayName, String bio, String contactInfo, String photoUrl) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", uid);
        profile.put("displayName", displayName);
        profile.put("bio", bio);
        profile.put("contactInfo", contactInfo);
        profile.put("photoUrl", photoUrl);
        // Keep rating and totalTransactions unchanged if already exists
        db.collection("users").document(uid).get().addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
                double rating = 0.0;
                int totalTransactions = 0;
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("rating")) {
                        rating = documentSnapshot.getDouble("rating");
                    }
                    if (documentSnapshot.contains("totalTransactions")) {
                        Long t = documentSnapshot.getLong("totalTransactions");
                        if (t != null) totalTransactions = t.intValue();
                    }
                }
                profile.put("rating", rating);
                profile.put("totalTransactions", totalTransactions);
                db.collection("users").document(uid).set(profile)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(UserProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(UserProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        // Delete user data from Firestore
        db.collection("users").document(uid).delete()
            .addOnSuccessListener(aVoid -> {
                // Delete user from FirebaseAuth
                user.delete()
                    .addOnSuccessListener(aVoid2 -> {
                        Toast.makeText(UserProfileActivity.this, "Account deleted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this, "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show());
            })
            .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void showDeactivateAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account? You can reactivate it by logging in again.")
            .setPositiveButton("Deactivate", (dialog, which) -> deactivateAccount())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deactivateAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        db.collection("users").document(uid)
            .update("isDeactivated", true)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(UserProfileActivity.this, "Account deactivated", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                Intent intent = new Intent(UserProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(UserProfileActivity.this, "Failed to deactivate account: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
