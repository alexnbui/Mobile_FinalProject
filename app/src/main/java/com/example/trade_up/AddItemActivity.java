package com.example.trade_up;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.List;

public class AddItemActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_LOCATION = 1002;
    private static final int MAX_PHOTOS = 10;

    private TextInputEditText etTitle, etDescription, etPrice, etLocation;
    private AutoCompleteTextView etCategory, etCondition;
    private Button btnAddPhoto, btnUseGPS, btnPreview, btnSubmit;
    private LinearLayout layoutPhotos;
    private ArrayList<String> photoUris = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etLocation = findViewById(R.id.etLocation);
        etCategory = findViewById(R.id.etCategory);
        etCondition = findViewById(R.id.etCondition);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnUseGPS = findViewById(R.id.btnUseGPS);
        btnPreview = findViewById(R.id.btnPreview);
        btnSubmit = findViewById(R.id.btnSubmit);
        layoutPhotos = findViewById(R.id.layoutPhotos);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupDropdowns();
        btnAddPhoto.setOnClickListener(v -> pickImage());
        btnUseGPS.setOnClickListener(v -> autofillLocation());
        btnPreview.setOnClickListener(v -> previewItem());
        btnSubmit.setOnClickListener(v -> submitItem());
    }

    private void setupDropdowns() {
        AutoCompleteTextView categoryView = findViewById(R.id.etCategory);
        AutoCompleteTextView conditionView = findViewById(R.id.etCondition);
        String[] categories = {"Electronics", "Books", "Clothing", "Home", "Other"};
        String[] conditions = {"New", "Like New", "Used", "For Parts"};
        categoryView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories));
        conditionView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, conditions));
    }

    private void pickImage() {
        if (photoUris.size() >= MAX_PHOTOS) {
            Toast.makeText(this, "You can add up to 10 photos.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            String uri = data.getData().toString();
            photoUris.add(uri);
            addPhotoThumbnail(uri);
        }
    }

    private void addPhotoThumbnail(String uri) {
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(android.net.Uri.parse(uri));
        layoutPhotos.addView(imageView);
    }

    private void autofillLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        // For demo: just set a placeholder
        etLocation.setText("Current Location (GPS)");
        // In production, use FusedLocationProviderClient to get real location
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autofillLocation();
        }
    }

    private void previewItem() {
        Toast.makeText(this, "Preview not implemented.", Toast.LENGTH_SHORT).show();
    }

    private void submitItem() {
        // Validate required fields
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String price = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String category = etCategory.getText().toString();
        String condition = etCondition.getText().toString();
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        if (title.isEmpty() || desc.isEmpty() || price.isEmpty() || category.isEmpty() || condition.isEmpty() || location.isEmpty() || photoUris.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields and add at least 1 photo.", Toast.LENGTH_SHORT).show();
            return;
        }
        btnSubmit.setEnabled(false);
        uploadImagesAndSaveItem(title, desc, price, category, condition, location);
    }

    private void uploadImagesAndSaveItem(String title, String desc, String price, String category, String condition, String location) {
        List<String> downloadUrls = new ArrayList<>();
        uploadImageRecursive(0, downloadUrls, title, desc, price, category, condition, location);
    }

    private void uploadImageRecursive(int index, List<String> downloadUrls, String title, String desc, String price, String category, String condition, String location) {
        if (index >= photoUris.size()) {
            saveItemToFirestore(title, desc, price, category, condition, location, downloadUrls);
            return;
        }
        String uriStr = photoUris.get(index);
        StorageReference ref = storage.getReference().child("item_images/" + System.currentTimeMillis() + "_" + index + ".jpg");
        ref.putFile(android.net.Uri.parse(uriStr))
            .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                downloadUrls.add(uri.toString());
                uploadImageRecursive(index + 1, downloadUrls, title, desc, price, category, condition, location);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to get image URL.", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            }))
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            });
    }

    private void saveItemToFirestore(String title, String desc, String price, String category, String condition, String location, List<String> imageUrls) {
        java.util.HashMap<String, Object> item = new java.util.HashMap<>();
        item.put("title", title);
        item.put("description", desc);
        item.put("price", price);
        item.put("category", category);
        item.put("condition", condition);
        item.put("location", location);
        item.put("images", imageUrls);
        item.put("timestamp", System.currentTimeMillis());
        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        item.put("sellerId", sellerId);
        db.collection("items").add(item)
            .addOnSuccessListener(documentReference -> {
                Toast.makeText(this, "Item submitted!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to submit item.", Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
            });
    }
}
