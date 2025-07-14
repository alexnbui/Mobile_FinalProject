package com.example.trade_up;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class ChatActivity extends AppCompatActivity {
    private LinearLayout layoutChatHeader;
    private ImageView imgChatUserAvatar;
    private TextView tvChatUserName;
    private FirebaseFirestore db;
    private String otherUserUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        layoutChatHeader = findViewById(R.id.layoutChatHeader);
        imgChatUserAvatar = findViewById(R.id.imgChatUserAvatar);
        tvChatUserName = findViewById(R.id.tvChatUserName);
        db = FirebaseFirestore.getInstance();

        otherUserUid = getIntent().getStringExtra("OTHER_USER_UID");
        loadOtherUserInfo(otherUserUid);

        layoutChatHeader.setOnClickListener(v -> {
            if (otherUserUid != null && !otherUserUid.isEmpty()) {
                Intent intent = new Intent(ChatActivity.this, PublicProfileActivity.class);
                intent.putExtra("USER_UID", otherUserUid);
                startActivity(intent);
            } else {
                Toast.makeText(this, "User info not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOtherUserInfo(String uid) {
        if (uid == null) return;
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayName = documentSnapshot.getString("displayName");
                    String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                    tvChatUserName.setText(displayName != null ? displayName : "");
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Picasso.get().load(profilePicUrl).placeholder(R.drawable.ic_person).into(imgChatUserAvatar);
                    } else {
                        imgChatUserAvatar.setImageResource(R.drawable.ic_person);
                    }
                }
            });
    }
}

