package com.example.agievent.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agievent.R;
import com.example.agievent.adapters.NewsPostAdapter;
import com.example.agievent.models.NewsPost;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsRoomActivity extends AppCompatActivity {
    private ListView listView;
    private NewsPostAdapter adapter;
    private FloatingActionButton fabCreatePost;
    private Button btnBack;

    private DatabaseReference postsRef;
    private ValueEventListener postsListener;
    private List<NewsPost> postsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_room);

        // Initialize Firebase reference
        postsRef = FirebaseDatabase.getInstance().getReference().child("posts");

        // Initialize posts list
        postsList = new ArrayList<>();

        // Initialize views
        listView = findViewById(R.id.lv_posts);
        fabCreatePost = findViewById(R.id.fab_create_post);
        btnBack = findViewById(R.id.btn_back);

        // Set up ListView
        adapter = new NewsPostAdapter(this, postsList);
        listView.setAdapter(adapter);

        // Set up ListView item click
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewsPost post = postsList.get(position);
                Intent intent = new Intent(NewsRoomActivity.this, PostDetailActivity.class);
                intent.putExtra("post_id", post.getId());
                startActivity(intent);
            }
        });

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Set up FAB for creating posts
        fabCreatePost.setOnClickListener(v -> showCreatePostDialog());

        // Load posts
        loadPosts();
    }

    private void loadPosts() {
        postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postsList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    NewsPost post = postSnapshot.getValue(NewsPost.class);
                    if (post != null) {
                        post.setId(postSnapshot.getKey());
                        postsList.add(post);
                    }
                }
                // Reverse to show newest first
                Collections.reverse(postsList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NewsRoomActivity.this, "Error loading posts: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        postsRef.orderByChild("timestamp").addValueEventListener(postsListener);
    }

    private void showCreatePostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_post, null);

        EditText edtContent = dialogView.findViewById(R.id.edt_post_content);
        Button btnPost = dialogView.findViewById(R.id.btn_post);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnPost.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (!content.isEmpty()) {
                createPost(content);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter some content", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void createPost(String content) {
        String postId = postsRef.push().getKey();
        if (postId == null) return;

        Map<String, Object> post = new HashMap<>();
        post.put("authorId", FirebaseAuth.getInstance().getCurrentUser().getUid());
        post.put("authorName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        post.put("content", content);
        post.put("timestamp", ServerValue.TIMESTAMP);

        postsRef.child(postId).setValue(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(NewsRoomActivity.this, "Post created!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NewsRoomActivity.this, "Error creating post",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postsListener != null) {
            postsRef.removeEventListener(postsListener);
        }
    }
}