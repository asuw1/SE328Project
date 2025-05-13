package com.example.projectdraft1.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectdraft1.R;
import com.example.projectdraft1.adapters.MessageAdapter;
import com.example.projectdraft1.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private EditText edtMessage;
    private Button btnSend, btnBack;
    private TextView txtRecipientName;

    private DatabaseReference messagesRef;
    private DatabaseReference chatRef;
    private DatabaseReference userChatsRef;
    private String chatId;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;
    private List<Message> messageList;
    private ValueEventListener messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data from intent
        chatId = getIntent().getStringExtra("chat_id");
        otherUserName = getIntent().getStringExtra("other_user_name");
        otherUserId = getIntent().getStringExtra("other_user_id");

        if (chatId == null || otherUserName == null || otherUserId == null) {
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("messages").child(chatId);
        chatRef = FirebaseDatabase.getInstance().getReference()
                .child("chats").child(chatId);
        userChatsRef = FirebaseDatabase.getInstance().getReference()
                .child("user_chats");

        // Initialize views
        recyclerView = findViewById(R.id.rv_messages);
        edtMessage = findViewById(R.id.edt_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        txtRecipientName = findViewById(R.id.txt_recipient_name);

        // Set recipient name
        txtRecipientName.setText(otherUserName);

        // Initialize message list
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();

        // Reset unread count
        resetUnreadCount();
    }

    private void loadMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();

                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messageList.add(message);
                    }
                }

                adapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this,
                        "Error loading messages: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    private void sendMessage() {
        String messageText = edtMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        // Create message
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("content", messageText);
        message.put("timestamp", ServerValue.TIMESTAMP);

        // Update messages
        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    // Clear input
                    edtMessage.setText("");

                    // Update chat last message
                    updateChatLastMessage(messageText);

                    // Update other user's unread count
                    incrementOtherUserUnreadCount();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ChatActivity.this,
                            "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatLastMessage(String messageText) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastTimestamp", ServerValue.TIMESTAMP);
        updates.put("lastSenderId", currentUserId);

        chatRef.updateChildren(updates);

        // Update user_chats timestamp
        userChatsRef.child(currentUserId).child(chatId)
                .child("timestamp").setValue(ServerValue.TIMESTAMP);
        userChatsRef.child(otherUserId).child(chatId)
                .child("timestamp").setValue(ServerValue.TIMESTAMP);
    }

    private void incrementOtherUserUnreadCount() {
        DatabaseReference otherUserChatRef = userChatsRef.child(otherUserId).child(chatId);

        otherUserChatRef.child("unreadCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Integer currentCount = dataSnapshot.getValue(Integer.class);
                if (currentCount == null) currentCount = 0;

                otherUserChatRef.child("unreadCount").setValue(currentCount + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void resetUnreadCount() {
        userChatsRef.child(currentUserId).child(chatId)
                .child("unreadCount").setValue(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}