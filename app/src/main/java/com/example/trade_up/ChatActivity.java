package com.example.trade_up;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private LinearLayout layoutChatHeader;
    private ImageView imgChatUserAvatar;
    private TextView tvChatUserName;
    private FirebaseFirestore db;
    private String otherUserUid;
    private RecyclerView recyclerViewMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnAttachImage;
    private ChatMessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private String chatId;
    private FirebaseAuth auth;
    private static final int REQUEST_IMAGE_PICK = 101;
    private Uri selectedImageUri;

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

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new ChatMessageAdapter(messageList, FirebaseAuth.getInstance().getUid());
        recyclerViewMessages.setAdapter(messageAdapter);
        auth = FirebaseAuth.getInstance();
        // Generate chatId (sorted UIDs)
        String myUid = auth.getUid();
        chatId = myUid.compareTo(otherUserUid) < 0 ? myUid + "_" + otherUserUid : otherUserUid + "_" + myUid;
        listenForMessages();
        btnSend.setOnClickListener(v -> sendMessage());
        btnAttachImage.setOnClickListener(v -> attachImage());
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

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        Message msg = new Message(auth.getUid(), otherUserUid, text, null, new Date().getTime());
        db.collection("chats").document(chatId).collection("messages").add(msg.toMap());
        etMessage.setText("");
    }

    private void attachImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            uploadImageAndSendMessage(selectedImageUri);
        }
    }

    private void uploadImageAndSendMessage(Uri imageUri) {
        if (imageUri == null) return;
        String myUid = auth.getUid();
        String fileName = "chat_images/" + chatId + "/" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.FirebaseStorage.getInstance().getReference()
            .child(fileName)
            .putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    Message msg = new Message(myUid, otherUserUid, null, uri.toString(), new Date().getTime());
                    db.collection("chats").document(chatId).collection("messages").add(msg.toMap());
                });
            });
    }

    private void listenForMessages() {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                for (DocumentChange dc : snap.getDocumentChanges()) {
                    if (dc.getType() == DocumentChange.Type.ADDED) {
                        Message msg = Message.fromDocument(dc.getDocument());
                        messageList.add(msg);
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                    }
                }
            });
    }

    private void checkIfBlocked() {
        String myUid = auth.getUid();
        db.collection("users").document(myUid)
            .collection("blocked").document(otherUserUid)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    etMessage.setEnabled(false);
                    btnSend.setEnabled(false);
                    btnAttachImage.setEnabled(false);
                    Toast.makeText(this, "You have blocked this user.", Toast.LENGTH_LONG).show();
                } else {
                    // Check if you are blocked by the other user
                    db.collection("users").document(otherUserUid)
                        .collection("blocked").document(myUid)
                        .get()
                        .addOnSuccessListener(otherDoc -> {
                            if (otherDoc.exists()) {
                                etMessage.setEnabled(false);
                                btnSend.setEnabled(false);
                                btnAttachImage.setEnabled(false);
                                Toast.makeText(this, "You are blocked by this user.", Toast.LENGTH_LONG).show();
                            }
                        });
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfBlocked();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_block) {
            blockUser();
            return true;
        } else if (item.getItemId() == R.id.action_report) {
            reportUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void blockUser() {
        String myUid = auth.getUid();
        db.collection("users").document(myUid)
            .collection("blocked").document(otherUserUid)
            .set(new java.util.HashMap<>())
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "User blocked", Toast.LENGTH_SHORT).show());
    }
    private void reportUser() {
        String myUid = auth.getUid();
        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("reporter", myUid);
        report.put("reported", otherUserUid);
        report.put("timestamp", System.currentTimeMillis());
        db.collection("reports").add(report)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show());
    }
    // Message model
    public static class Message {
        public String senderId, receiverId, text, imageUrl;
        public long timestamp;
        public Message() {}
        public Message(String senderId, String receiverId, String text, String imageUrl, long timestamp) {
            this.senderId = senderId; this.receiverId = receiverId; this.text = text; this.imageUrl = imageUrl; this.timestamp = timestamp;
        }
        public java.util.Map<String, Object> toMap() {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("senderId", senderId);
            map.put("receiverId", receiverId);
            map.put("text", text);
            map.put("imageUrl", imageUrl);
            map.put("timestamp", timestamp);
            return map;
        }
        public static Message fromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
            Message m = new Message();
            m.senderId = doc.getString("senderId");
            m.receiverId = doc.getString("receiverId");
            m.text = doc.getString("text");
            m.imageUrl = doc.getString("imageUrl");
            m.timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
            return m;
        }
    }
    // RecyclerView Adapter for chat messages
    public static class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Message> messages;
        private final String myUid;
        public ChatMessageAdapter(List<Message> messages, String myUid) {
            this.messages = messages;
            this.myUid = myUid;
        }
        @Override
        public int getItemViewType(int position) {
            return messages.get(position).senderId.equals(myUid) ? 1 : 0;
        }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(
                viewType == 1 ? R.layout.item_message_sent : R.layout.item_message_received, parent, false);
            return new MessageVH(v);
        }
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Message msg = messages.get(position);
            MessageVH vh = (MessageVH) holder;
            if (!TextUtils.isEmpty(msg.text)) {
                vh.tvMessage.setText(msg.text);
                vh.tvMessage.setVisibility(View.VISIBLE);
            } else {
                vh.tvMessage.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(msg.imageUrl)) {
                vh.imgMessage.setVisibility(View.VISIBLE);
                Picasso.get().load(msg.imageUrl).into(vh.imgMessage);
            } else {
                vh.imgMessage.setVisibility(View.GONE);
            }
        }
        @Override
        public int getItemCount() { return messages.size(); }
        static class MessageVH extends RecyclerView.ViewHolder {
            TextView tvMessage;
            ImageView imgMessage;
            MessageVH(View v) {
                super(v);
                tvMessage = v.findViewById(R.id.tvMessage);
                imgMessage = v.findViewById(R.id.imgMessage);
            }
        }
    }
    // TODO: Add block/report user logic
}
