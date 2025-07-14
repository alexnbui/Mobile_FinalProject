package com.example.trade_up;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imgItemPhotoPreview;
    private Button btnAddPhoto, btnSubmitItem, btnPreviewItem;
    private EditText etItemTitle, etItemDescription, etItemPrice, etItemCategory, etItemCondition, etItemLocation;
    private Spinner spinnerStatus;
    private Uri imageUri;
    private String imageUrl;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private boolean isEditMode = false;
    private String editItemId = null;
    private String originalImageUrl = null;
    private String[] statusOptions = {"Available", "Sold", "Paused"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);
        imgItemPhotoPreview = findViewById(R.id.imgItemPhotoPreview);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnSubmitItem = findViewById(R.id.btnSubmitItem);
        btnPreviewItem = findViewById(R.id.btnPreviewItem);
        etItemTitle = findViewById(R.id.etItemTitle);
        etItemDescription = findViewById(R.id.etItemDescription);
        etItemPrice = findViewById(R.id.etItemPrice);
        etItemCategory = findViewById(R.id.etItemCategory);
        etItemCondition = findViewById(R.id.etItemCondition);
        etItemLocation = findViewById(R.id.etItemLocation);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("item_photos");
        mAuth = FirebaseAuth.getInstance();

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("EDIT_MODE", false);
        if (isEditMode) {
            editItemId = intent.getStringExtra("ITEM_ID");
            btnSubmitItem.setText("Update Item");
            loadItemForEdit();
        }

        btnAddPhoto.setOnClickListener(v -> openFileChooser());
        btnSubmitItem.setOnClickListener(v -> submitItem());
        btnPreviewItem.setOnClickListener(v -> showPreviewDialog());
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
                imgItemPhotoPreview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadItemForEdit() {
        if (editItemId == null) return;
        db.collection("items").document(editItemId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    etItemTitle.setText(documentSnapshot.getString("title"));
                    etItemDescription.setText(documentSnapshot.getString("description"));
                    etItemPrice.setText(documentSnapshot.getDouble("price") != null ? String.valueOf(documentSnapshot.getDouble("price")) : "");
                    etItemCategory.setText(documentSnapshot.getString("category"));
                    etItemCondition.setText(documentSnapshot.getString("condition"));
                    etItemLocation.setText(documentSnapshot.getString("location"));
                    originalImageUrl = documentSnapshot.getString("photoUrl");
                    String status = documentSnapshot.getString("status");
                    if (status != null) {
                        for (int i = 0; i < statusOptions.length; i++) {
                            if (statusOptions[i].equalsIgnoreCase(status)) {
                                spinnerStatus.setSelection(i);
                                break;
                            }
                        }
                    }
                    if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                        Picasso.get().load(originalImageUrl).placeholder(R.drawable.ic_launcher_background).into(imgItemPhotoPreview);
                    }
                }
            });
    }

    private void submitItem() {
        String title = etItemTitle.getText().toString().trim();
        String description = etItemDescription.getText().toString().trim();
        String priceStr = etItemPrice.getText().toString().trim();
        String category = etItemCategory.getText().toString().trim();
        String condition = etItemCondition.getText().toString().trim();
        String location = etItemLocation.getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr)
                || TextUtils.isEmpty(category) || TextUtils.isEmpty(condition) || TextUtils.isEmpty(location)
                || (!isEditMode && imageUri == null && (originalImageUrl == null || originalImageUrl.isEmpty()))) {
            Toast.makeText(this, "Please fill all required fields and add a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitItem.setEnabled(false);
        if (isEditMode) {
            if (imageUri != null) {
                uploadImageAndUpdateItem(title, description, price, category, condition, location, status);
            } else {
                updateItemInFirestore(title, description, price, category, condition, location, originalImageUrl, status);
            }
        } else {
            uploadImageAndSaveItem(title, description, price, category, condition, location, status);
        }
    }

    private void uploadImageAndSaveItem(String title, String description, double price, String category, String condition, String location, String status) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            btnSubmitItem.setEnabled(true);
            return;
        }
        String uid = user.getUid();
        final StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrl = uri.toString();
                    saveItemToFirestore(uid, title, description, price, category, condition, location, imageUrl, status);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    btnSubmitItem.setEnabled(true);
                });
    }

    private void uploadImageAndUpdateItem(String title, String description, double price, String category, String condition, String location, String status) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            btnSubmitItem.setEnabled(true);
            return;
        }
        final StorageReference fileRef = storageRef.child(System.currentTimeMillis() + ".jpg");
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrl = uri.toString();
                    updateItemInFirestore(title, description, price, category, condition, location, imageUrl, status);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    btnSubmitItem.setEnabled(true);
                });
    }

    private void saveItemToFirestore(String uid, String title, String description, double price, String category, String condition, String location, String imageUrl, String status) {
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("category", category);
        item.put("condition", condition);
        item.put("location", location);
        item.put("photoUrl", imageUrl);
        item.put("sellerUid", uid);
        item.put("status", status);
        item.put("timestamp", System.currentTimeMillis());
        item.put("views", 0);
        item.put("interactions", 0);
        db.collection("items").add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddItemActivity.this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
                    btnSubmitItem.setEnabled(true);
                });
    }

    private void updateItemInFirestore(String title, String description, double price, String category, String condition, String location, String imageUrl, String status) {
        if (editItemId == null) return;
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("category", category);
        item.put("condition", condition);
        item.put("location", location);
        item.put("photoUrl", imageUrl);
        item.put("status", status);
        db.collection("items").document(editItemId)
                .update(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddItemActivity.this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to update item", Toast.LENGTH_SHORT).show();
                    btnSubmitItem.setEnabled(true);
                });
    }

    private void showPreviewDialog() {
        View previewView = LayoutInflater.from(this).inflate(R.layout.activity_item_detail, null);
        ImageView img = previewView.findViewById(R.id.imgItemPhoto);
        TextView title = previewView.findViewById(R.id.tvItemTitle);
        TextView price = previewView.findViewById(R.id.tvItemPrice);
        TextView desc = previewView.findViewById(R.id.tvItemDescription);
        TextView cat = previewView.findViewById(R.id.tvItemCategory);
        TextView cond = previewView.findViewById(R.id.tvItemCondition);
        // Set preview data
        title.setText(etItemTitle.getText().toString());
        price.setText(etItemPrice.getText().toString());
        desc.setText(etItemDescription.getText().toString());
        cat.setText(etItemCategory.getText().toString());
        cond.setText(etItemCondition.getText().toString());
        if (imageUri != null) {
            img.setImageURI(imageUri);
        } else if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
            Picasso.get().load(originalImageUrl).into(img);
        }
        new AlertDialog.Builder(this)
            .setTitle("Preview Item")
            .setView(previewView)
            .setPositiveButton("OK", null)
            .show();
    }
}
