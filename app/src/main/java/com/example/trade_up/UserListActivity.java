package com.example.trade_up;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {
    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userList);
        recyclerViewUsers.setAdapter(userAdapter);
        db = FirebaseFirestore.getInstance();
        loadUsers();
    }

    private void loadUsers() {
        db.collection("users").get().addOnSuccessListener(queryDocumentSnapshots -> {
            userList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String uid = doc.getId();
                String displayName = doc.getString("displayName");
                String profilePicUrl = doc.getString("profilePicUrl");
                Double rating = doc.getDouble("rating");
                userList.add(new User(uid, displayName, profilePicUrl, rating));
            }
            userAdapter.notifyDataSetChanged();
        });
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users;
        UserAdapter(List<User> users) { this.users = users; }
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
            return new UserViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvName.setText(user.displayName != null ? user.displayName : "");
            holder.tvRating.setText("Rating: " + (user.rating != null ? String.format("%.1f", user.rating) : "0.0"));
            if (user.profilePicUrl != null && !user.profilePicUrl.isEmpty()) {
                Picasso.get().load(user.profilePicUrl).placeholder(R.drawable.ic_person).into(holder.imgAvatar);
            } else {
                holder.imgAvatar.setImageResource(R.drawable.ic_person);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(UserListActivity.this, PublicProfileActivity.class);
                intent.putExtra("USER_UID", user.uid);
                startActivity(intent);
            });
        }
        @Override
        public int getItemCount() { return users.size(); }
        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView tvName, tvRating;
            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvRating = itemView.findViewById(R.id.tvUserRating);
            }
        }
    }

    private static class User {
        String uid, displayName, profilePicUrl;
        Double rating;
        User(String uid, String displayName, String profilePicUrl, Double rating) {
            this.uid = uid;
            this.displayName = displayName;
            this.profilePicUrl = profilePicUrl;
            this.rating = rating;
        }
    }
}

