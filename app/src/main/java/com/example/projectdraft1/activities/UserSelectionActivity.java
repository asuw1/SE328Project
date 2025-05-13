package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdraft1.R;
import com.example.projectdraft1.adapters.UserSelectionAdapter;
import com.example.projectdraft1.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSelectionActivity extends AppCompatActivity {
    private ListView listView;
    private UserSelectionAdapter adapter;
    private Button btnBack;

    private DatabaseReference usersRef;
    private DatabaseReference chatsRef;
    private DatabaseReference userChatsRef;
    private String currentUserId;
    private String currentUserName;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_selection);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");
        userChatsRef = FirebaseDatabase.getInstance().getReference().child("user_chats");

        // Initialize views
        listView = findViewById(R.id.lv_users);
        btnBack = findViewById(R.id.btn_back);

        // Initialize user list
        userList = new ArrayList<>();
        adapter = new UserSelectionAdapter(this, userList);
        listView.setAdapter(adapter);

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);
            checkOrCreateChat(selectedUser);
        });

        // Load users
        loadUsers();
    }

    private void loadUsers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();

                    // Skip current user
                    if (userId.equals(currentUserId)) {
                        continue;
                    }

                    String name = userSnapshot.child("name").getValue(String.class);
                    String email = userSnapshot.child("email").getValue(String.class);

                    User user = new User();
                    user.setUserId(userId);
                    user.setName(name);
                    user.setEmail(email);

                    userList.add(user);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserSelectionActivity.this,
                        "Error loading users: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkOrCreateChat(User selectedUser) {
        // Check if chat already exists
        userChatsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String existingChatId = null;

                // Check if we already have a chat with this user
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    String otherUserId = chatSnapshot.child("otherUserId").getValue(String.class);
                    if (selectedUser.getUserId().equals(otherUserId)) {
                        existingChatId = chatSnapshot.getKey();
                        break;
                    }
                }

                if (existingChatId != null) {
                    // Chat exists, navigate to it
                    navigateToChat(existingChatId, selectedUser.getName(), selectedUser.getUserId());
                } else {
                    // Create new chat
                    createNewChat(selectedUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserSelectionActivity.this,
                        "Error checking chats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewChat(User selectedUser) {
        String chatId = chatsRef.push().getKey();
        if (chatId == null) return;

        // Create chat document with proper structure
        Map<String, Object> chatData = new HashMap<>();

        // Create participants array
        Map<String, Object> participants = new HashMap<>();
        participants.put("0", currentUserId);
        participants.put("1", selectedUser.getUserId());
        chatData.put("participants", participants);

        // Create participant names map
        Map<String, Object> participantNames = new HashMap<>();
        participantNames.put(currentUserId, currentUserName);
        participantNames.put(selectedUser.getUserId(), selectedUser.getName());
        chatData.put("participantNames", participantNames);

        chatData.put("lastMessage", "");
        chatData.put("lastTimestamp", System.currentTimeMillis());

        // Create user_chats entries
        Map<String, Object> currentUserChatData = new HashMap<>();
        currentUserChatData.put("otherUserName", selectedUser.getName());
        currentUserChatData.put("otherUserId", selectedUser.getUserId());
        currentUserChatData.put("unreadCount", 0);

        Map<String, Object> otherUserChatData = new HashMap<>();
        otherUserChatData.put("otherUserName", currentUserName);
        otherUserChatData.put("otherUserId", currentUserId);
        otherUserChatData.put("unreadCount", 0);

        // Execute all updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("/chats/" + chatId, chatData);
        updates.put("/user_chats/" + currentUserId + "/" + chatId, currentUserChatData);
        updates.put("/user_chats/" + selectedUser.getUserId() + "/" + chatId, otherUserChatData);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    navigateToChat(chatId, selectedUser.getName(), selectedUser.getUserId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UserSelectionActivity.this,
                            "Failed to create chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UserSelection", "Error creating chat", e);
                });
    }

    private void navigateToChat(String chatId, String otherUserName, String otherUserId) {

        Intent intent = new Intent(UserSelectionActivity.this, ChatActivity.class);
        intent.putExtra("chat_id", chatId);
        intent.putExtra("other_user_name", otherUserName);
        intent.putExtra("other_user_id", otherUserId);
        startActivity(intent);
        finish();
    }
}