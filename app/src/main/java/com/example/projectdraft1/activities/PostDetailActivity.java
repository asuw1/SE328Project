package com.example.projectdraft1.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdraft1.R;
import com.example.projectdraft1.adapters.ReplyAdapter;
import com.example.projectdraft1.models.NewsPost;
import com.example.projectdraft1.models.Reply;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {
    private TextView txtAuthor, txtContent, txtTimestamp;
    private EditText edtReply;
    private Button btnSendReply, btnBack;
    private ListView lvReplies;

    private DatabaseReference postRef;
    private DatabaseReference repliesRef;
    private String postId;

    private ReplyAdapter replyAdapter;
    private List<Reply> replyList;
    private ValueEventListener repliesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // Get post ID from intent
        postId = getIntent().getStringExtra("post_id");
        if (postId == null) {
            finish();
            return;
        }

        // Initialize Firebase references
        postRef = FirebaseDatabase.getInstance().getReference("posts").child(postId);
        repliesRef = FirebaseDatabase.getInstance().getReference("replies").child(postId);

        // Initialize views
        txtAuthor = findViewById(R.id.txt_author);
        txtContent = findViewById(R.id.txt_content);
        txtTimestamp = findViewById(R.id.txt_timestamp);
        edtReply = findViewById(R.id.edt_reply);
        btnSendReply = findViewById(R.id.btn_send_reply);
        btnBack = findViewById(R.id.btn_back);
        lvReplies = findViewById(R.id.lv_replies);

        // Initialize reply list and adapter
        replyList = new ArrayList<>();
        replyAdapter = new ReplyAdapter(this, replyList);
        lvReplies.setAdapter(replyAdapter);

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());
        btnSendReply.setOnClickListener(v -> sendReply());

        // Load post details
        loadPostDetails();

        // Load replies
        loadReplies();
    }

    private void loadPostDetails() {
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    NewsPost post = dataSnapshot.getValue(NewsPost.class);
                    if (post != null) {
                        txtAuthor.setText(post.getAuthorName());
                        txtContent.setText(post.getContent());
                        txtTimestamp.setText(formatTimestamp(post.getTimestamp()));
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, "Post not found",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "Error loading post",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReplies() {
        repliesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                replyList.clear();
                for (DataSnapshot replySnapshot : dataSnapshot.getChildren()) {
                    Reply reply = replySnapshot.getValue(Reply.class);
                    if (reply != null) {
                        reply.setId(replySnapshot.getKey());
                        replyList.add(reply);
                    }
                }
                replyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostDetailActivity.this, "Error loading replies",
                        Toast.LENGTH_SHORT).show();
            }
        };

        repliesRef.orderByChild("timestamp").addValueEventListener(repliesListener);
    }

    private void sendReply() {
        String replyContent = edtReply.getText().toString().trim();
        if (replyContent.isEmpty()) {
            Toast.makeText(this, "Please enter a reply", Toast.LENGTH_SHORT).show();
            return;
        }

        String replyId = repliesRef.push().getKey();
        if (replyId == null) return;

        Map<String, Object> reply = new HashMap<>();
        reply.put("authorId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        reply.put("authorName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        reply.put("content", replyContent);
        reply.put("timestamp", ServerValue.TIMESTAMP);

        repliesRef.child(replyId).setValue(reply)
                .addOnSuccessListener(aVoid -> {
                    edtReply.setText("");
                    Toast.makeText(this, "Reply posted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post reply", Toast.LENGTH_SHORT).show();
                });
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (repliesListener != null) {
            repliesRef.removeEventListener(repliesListener);
        }
    }
}