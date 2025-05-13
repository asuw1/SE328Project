package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdraft1.R;
import com.example.projectdraft1.adapters.ChatListAdapter;
import com.example.projectdraft1.models.Chat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {
    private ListView listView;
    private ChatListAdapter adapter;
    private Button btnBack;
    private FloatingActionButton fabNewChat;

    private DatabaseReference userChatsRef;
    private DatabaseReference chatsRef;
    private String currentUserId;
    private List<Chat> chatList;
    private ValueEventListener userChatsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userChatsRef = FirebaseDatabase.getInstance().getReference()
                .child("user_chats").child(currentUserId);
        chatsRef = FirebaseDatabase.getInstance().getReference().child("chats");

        // Initialize views
        listView = findViewById(R.id.lv_chats);
        btnBack = findViewById(R.id.btn_back);
        fabNewChat = findViewById(R.id.fab_new_chat);

        // Initialize chat list
        chatList = new ArrayList<>();
        adapter = new ChatListAdapter(this, chatList);
        listView.setAdapter(adapter);

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());

        fabNewChat.setOnClickListener(v -> {
            // Navigate to user selection activity
            Intent intent = new Intent(ChatListActivity.this, UserSelectionActivity.class);
            startActivity(intent);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Chat chat = chatList.get(position);
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("chat_id", chat.getChatId());
            intent.putExtra("other_user_name", chat.getOtherUserName());
            intent.putExtra("other_user_id", chat.getOtherUserId());
            startActivity(intent);
        });

        // Load chats
        loadChats();
    }

    private void loadChats() {
        userChatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                adapter.notifyDataSetChanged();

                if (!dataSnapshot.exists()) {
                    return;
                }

                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    final String chatId = chatSnapshot.getKey();
                    final String otherUserName = chatSnapshot.child("otherUserName").getValue(String.class);
                    final String otherUserId = chatSnapshot.child("otherUserId").getValue(String.class);
                    Integer unreadCount = chatSnapshot.child("unreadCount").getValue(Integer.class);

                    if (unreadCount == null) unreadCount = 0;
                    final int finalUnreadCount = unreadCount;

                    // Get chat details
                    chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot chatDataSnapshot) {
                            if (chatDataSnapshot.exists()) {
                                String lastMessage = chatDataSnapshot.child("lastMessage").getValue(String.class);
                                Long lastTimestamp = chatDataSnapshot.child("lastTimestamp").getValue(Long.class);

                                // Check if this chat already exists in the list
                                boolean alreadyExists = false;
                                for (Chat existingChat : chatList) {
                                    if (existingChat.getChatId().equals(chatId)) {
                                        alreadyExists = true;
                                        break;
                                    }
                                }

                                if (!alreadyExists) {
                                    Chat chat = new Chat();
                                    chat.setChatId(chatId);
                                    chat.setOtherUserName(otherUserName);
                                    chat.setOtherUserId(otherUserId);
                                    chat.setLastMessage(lastMessage != null ? lastMessage : "");
                                    chat.setLastTimestamp(lastTimestamp != null ? lastTimestamp : 0);
                                    chat.setUnreadCount(finalUnreadCount);

                                    chatList.add(chat);

                                    // Sort by timestamp (newest first)
                                    Collections.sort(chatList, (a, b) ->
                                            Long.compare(b.getLastTimestamp(), a.getLastTimestamp()));

                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(ChatListActivity.this,
                                    "Error loading chat: " + databaseError.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatListActivity.this,
                        "Error loading chats: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        userChatsRef.addValueEventListener(userChatsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userChatsListener != null) {
            userChatsRef.removeEventListener(userChatsListener);
        }
    }
}