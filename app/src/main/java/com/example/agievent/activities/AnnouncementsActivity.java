package com.example.agievent.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agievent.R;
import com.example.agievent.adapters.AnnouncementAdapter;
import com.example.agievent.models.Announcement;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnouncementsActivity extends AppCompatActivity {
    private ListView listView;
    private AnnouncementAdapter adapter;
    private FloatingActionButton fabAddAnnouncement;
    private Button btnBack;

    private DatabaseReference announcementsRef;
    private DatabaseReference userRef;
    private ValueEventListener announcementsListener;
    private List<Announcement> announcementsList;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);

        // Initialize Firebase references
        announcementsRef = FirebaseDatabase.getInstance().getReference().child("announcements");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

        // Initialize list
        announcementsList = new ArrayList<>();

        // Initialize views
        listView = findViewById(R.id.lv_announcements);
        fabAddAnnouncement = findViewById(R.id.fab_add_announcement);
        btnBack = findViewById(R.id.btn_back);

        // Set up ListView
        adapter = new AnnouncementAdapter(this, announcementsList);
        listView.setAdapter(adapter);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Check admin status and set up FAB
        checkAdminStatus();

        // Load announcements
        loadAnnouncements();
    }

    private void checkAdminStatus() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    isAdmin = Boolean.TRUE.equals(dataSnapshot.child("isAdmin").getValue(Boolean.class));
                    if (isAdmin) {
                        fabAddAnnouncement.setVisibility(View.VISIBLE);
                        fabAddAnnouncement.setOnClickListener(v -> showAddAnnouncementDialog());
                    } else {
                        fabAddAnnouncement.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AnnouncementsActivity.this, "Error checking admin status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAnnouncements() {
        announcementsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                announcementsList.clear();
                for (DataSnapshot announcementSnapshot : dataSnapshot.getChildren()) {
                    Announcement announcement = announcementSnapshot.getValue(Announcement.class);
                    if (announcement != null) {
                        announcement.setId(announcementSnapshot.getKey());
                        announcementsList.add(announcement);
                    }
                }
                // Sort by timestamp (newest first) and then by priority
                Collections.sort(announcementsList, (a, b) -> {
                    // First sort by priority (urgent first)
                    int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    // Then sort by timestamp (newest first)
                    return Long.compare(b.getTimestamp(), a.getTimestamp());
                });
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AnnouncementsActivity.this, "Error loading announcements",
                        Toast.LENGTH_SHORT).show();
            }
        };

        announcementsRef.addValueEventListener(announcementsListener);
    }

    private void showAddAnnouncementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_announcement, null);

        EditText edtTitle = dialogView.findViewById(R.id.edt_title);
        EditText edtContent = dialogView.findViewById(R.id.edt_content);
        RadioGroup radioGroupPriority = dialogView.findViewById(R.id.radio_group_priority);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.setView(dialogView).create();

        btnAdd.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String content = edtContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected priority
            int priority = 0; // Default to normal
            int selectedId = radioGroupPriority.getCheckedRadioButtonId();
            if (selectedId == R.id.radio_important) {
                priority = 1;
            } else if (selectedId == R.id.radio_urgent) {
                priority = 2;
            }

            createAnnouncement(title, content, priority);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void createAnnouncement(String title, String content, int priority) {
        String announcementId = announcementsRef.push().getKey();
        if (announcementId == null) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String authorId = auth.getCurrentUser().getUid();
        String authorName = auth.getCurrentUser().getDisplayName();
        if (authorName == null || authorName.isEmpty()) {
            authorName = "Admin";
        }

        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("content", content);
        announcement.put("authorId", authorId);
        announcement.put("authorName", authorName);
        announcement.put("priority", priority);
        announcement.put("timestamp", System.currentTimeMillis());

        announcementsRef.child(announcementId).setValue(announcement)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AnnouncementsActivity.this, "Announcement posted!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AnnouncementsActivity.this, "Error posting announcement",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (announcementsListener != null) {
            announcementsRef.removeEventListener(announcementsListener);
        }
    }
}