package com.example.trade_up;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 2;
    private RecyclerView rvItemPhotos;
    private List<Uri> imageUris = new ArrayList<>();
    private ItemPhotoAdapter photoAdapter;
    private Button btnAddPhoto, btnSubmitItem, btnPreviewItem, btnAutofillLocation;
    private EditText etItemTitle, etItemDescription, etItemPrice, etItemCategory, etItemCondition, etItemLocation, etItemBehavior, etAdditionalTags;
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
        rvItemPhotos = findViewById(R.id.rvItemPhotos);
        photoAdapter = new ItemPhotoAdapter(imageUris);
        rvItemPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvItemPhotos.setAdapter(photoAdapter);
        btnAddPhoto = findViewById(R.id.btnAddPhoto);
        btnSubmitItem = findViewById(R.id.btnSubmitItem);
        btnPreviewItem = findViewById(R.id.btnPreviewItem);
        btnAutofillLocation = findViewById(R.id.btnAutofillLocation);
        etItemTitle = findViewById(R.id.etItemTitle);
        etItemDescription = findViewById(R.id.etItemDescription);
        etItemPrice = findViewById(R.id.etItemPrice);
        etItemCategory = findViewById(R.id.etItemCategory);
        etItemCondition = findViewById(R.id.etItemCondition);
        etItemLocation = findViewById(R.id.etItemLocation);
        etItemBehavior = findViewById(R.id.etItemBehavior);
        etAdditionalTags = findViewById(R.id.etAdditionalTags);
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
        btnAutofillLocation.setOnClickListener(v -> autofillLocation());
        btnSubmitItem.setOnClickListener(v -> submitItem());
        btnPreviewItem.setOnClickListener(v -> showPreviewDialog());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count && imageUris.size() < 10; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        if (isValidImageType(uri)) imageUris.add(uri);
                    }
                } else if (data.getData() != null && imageUris.size() < 10) {
                    Uri uri = data.getData();
                    if (isValidImageType(uri)) imageUris.add(uri);
                }
                photoAdapter.notifyDataSetChanged();
            }
        }
    }

    private boolean isValidImageType(Uri uri) {
        String type = getContentResolver().getType(uri);
        return type != null && (type.equals("image/jpeg") || type.equals("image/png"));
    }

    private void autofillLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                etItemLocation.setText(location.getLatitude() + ", " + location.getLongitude());
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            autofillLocation();
        }
    }

    private void handleImageOrientation(Uri uri, Bitmap bitmap) {
        try {
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            android.database.Cursor cursor = getContentResolver().query(uri, filePathColumn, null, null, null);
            String picturePath = null;
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    picturePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
            if (picturePath != null) {
                ExifInterface exif = new ExifInterface(picturePath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                    default:
                        break;
                }
                if (orientation != ExifInterface.ORIENTATION_NORMAL) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        String itemBehavior = etItemBehavior.getText().toString().trim();
        String additionalTags = etAdditionalTags.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr)
                || TextUtils.isEmpty(category) || TextUtils.isEmpty(condition) || TextUtils.isEmpty(location)
                || (!isEditMode && imageUris.size() == 0)) {
            Toast.makeText(this, "Please fill all required fields and add at least 1 photo", Toast.LENGTH_SHORT).show();
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
            // For simplicity, only allow updating text fields and not images in edit mode for now
            updateItemInFirestore(title, description, price, category, condition, location, status, itemBehavior, additionalTags, null);
        } else {
            uploadImagesAndSaveItem(title, description, price, category, condition, location, status, itemBehavior, additionalTags);
        }
    }

    private void uploadImagesAndSaveItem(String title, String description, double price, String category, String condition, String location, String status, String itemBehavior, String additionalTags) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            btnSubmitItem.setEnabled(true);
            return;
        }
        String uid = user.getUid();
        List<String> uploadedUrls = new ArrayList<>();
        if (imageUris.size() == 0) {
            Toast.makeText(this, "Please add at least 1 photo", Toast.LENGTH_SHORT).show();
            btnSubmitItem.setEnabled(true);
            return;
        }
        uploadNextImage(0, uploadedUrls, uid, title, description, price, category, condition, location, status, itemBehavior, additionalTags);
    }

    private void uploadNextImage(int index, List<String> uploadedUrls, String uid, String title, String description, double price, String category, String condition, String location, String status, String itemBehavior, String additionalTags) {
        if (index >= imageUris.size()) {
            saveItemToFirestore(uid, title, description, price, category, condition, location, uploadedUrls, status, itemBehavior, additionalTags);
            return;
        }
        Uri uri = imageUris.get(index);
        final StorageReference fileRef = storageRef.child(System.currentTimeMillis() + "_" + index + ".jpg");
        fileRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    uploadedUrls.add(downloadUri.toString());
                    uploadNextImage(index + 1, uploadedUrls, uid, title, description, price, category, condition, location, status, itemBehavior, additionalTags);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(AddItemActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    btnSubmitItem.setEnabled(true);
                });
    }

    private void saveItemToFirestore(String uid, String title, String description, double price, String category, String condition, String location, List<String> imageUrls, String status, String itemBehavior, String additionalTags) {
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("category", category);
        item.put("condition", condition);
        item.put("location", location);
        item.put("photoUrls", imageUrls);
        item.put("sellerUid", uid);
        item.put("status", status);
        item.put("timestamp", System.currentTimeMillis());
        item.put("views", 0);
        item.put("interactions", 0);
        if (!TextUtils.isEmpty(itemBehavior)) item.put("itemBehavior", itemBehavior);
        if (!TextUtils.isEmpty(additionalTags)) item.put("additionalTags", additionalTags);
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

    private void updateItemInFirestore(String title, String description, double price, String category, String condition, String location, String status, String itemBehavior, String additionalTags, List<String> imageUrls) {
        if (editItemId == null) return;
        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("price", price);
        item.put("category", category);
        item.put("condition", condition);
        item.put("location", location);
        item.put("status", status);
        if (!TextUtils.isEmpty(itemBehavior)) item.put("itemBehavior", itemBehavior);
        if (!TextUtils.isEmpty(additionalTags)) item.put("additionalTags", additionalTags);
        if (imageUrls != null) item.put("photoUrls", imageUrls);
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
