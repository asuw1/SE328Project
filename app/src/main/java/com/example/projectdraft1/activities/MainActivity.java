package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.projectdraft1.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    // UI Elements
    private TextView txtWelcome;
    private CardView cardProfile, cardNewsroom, cardMessages, cardAgenda;
    private Button btnAdmin;
    private FloatingActionButton fabAnnouncements;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        initializeUI();

        // Check if user is logged in
        checkUserAuthentication();

        // Setup dashboard cards
        setupNavigationCards();

        // Check if user is admin
        checkAdminStatus();

        // Load latest announcement if available
        loadLatestAnnouncement();
    }

    private void initializeUI() {
        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // Find views
        txtWelcome = findViewById(R.id.txt_welcome);
        cardProfile = findViewById(R.id.card_profile);
        cardNewsroom = findViewById(R.id.card_newsroom);
        cardMessages = findViewById(R.id.card_messages);
        cardAgenda = findViewById(R.id.card_agenda);
        btnAdmin = findViewById(R.id.btn_admin);
        fabAnnouncements = findViewById(R.id.fab_announcements);

        // Hide admin button by default
        btnAdmin.setVisibility(View.GONE);
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Not logged in, go to login screen
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // User is logged in, update welcome message
        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            txtWelcome.setText(getString(R.string.welcome_user, displayName));
        } else {
            // Fetch user name from database if not in Firebase Auth
            mDatabase.child("users").child(currentUser.getUid()).child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String name = dataSnapshot.getValue(String.class);
                                txtWelcome.setText(getString(R.string.welcome_user, name));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Use default welcome message
                            txtWelcome.setText(R.string.welcome);
                        }
                    });
        }
    }

    private void setupNavigationCards() {
        cardProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        cardNewsroom.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewsRoomActivity.class));
        });

        cardMessages.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ChatListActivity.class));
        });

        cardAgenda.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AgendaActivity.class));
        });

        fabAnnouncements.setOnClickListener(v -> {
            // Show dialog with latest announcements
            showAnnouncementsDialog();
        });
    }

    private void checkAdminStatus() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("isAdmin")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class))) {
                            // User is an admin, show admin button
                            isAdmin = true;
                            btnAdmin.setVisibility(View.VISIBLE);
                            btnAdmin.setOnClickListener(v -> {
                                startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Error checking admin status",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadLatestAnnouncement() {
        // Query for the latest announcement
        mDatabase.child("announcements")
                .orderByChild("timestamp")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            // Make announcement FAB visible
                            fabAnnouncements.setVisibility(View.VISIBLE);

                            // Check if user has seen this announcement
                            for (DataSnapshot announcementSnap : dataSnapshot.getChildren()) {
                                String announcementId = announcementSnap.getKey();
                                Long timestamp = announcementSnap.child("timestamp").getValue(Long.class);

                                // Check if the user has seen this announcement
                                checkAnnouncementSeen(announcementId, timestamp);
                                break; // Only need to check the latest one
                            }
                        } else {
                            // No announcements yet
                            fabAnnouncements.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Error loading announcement
                        fabAnnouncements.setVisibility(View.GONE);
                    }
                });
    }

    private void checkAnnouncementSeen(String announcementId, Long timestamp) {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("seen_announcements").child(announcementId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            // User hasn't seen this announcement
                            fabAnnouncements.setImageResource(R.drawable.ic_announcement_new);
                        } else {
                            // User has seen this announcement
                            fabAnnouncements.setImageResource(R.drawable.ic_announcement);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Default to regular icon
                        fabAnnouncements.setImageResource(R.drawable.ic_announcement);
                    }
                });
    }

    private void showAnnouncementsDialog() {
        // Implementation for showing announcements
        // You could launch a new activity or show a dialog

        // Here we'll open an activity with the latest announcements
        Intent intent = new Intent(MainActivity.this, AnnouncementsActivity.class);
        startActivity(intent);

        // Mark announcements as seen in the database
        markAnnouncementsAsSeen();
    }

    private void markAnnouncementsAsSeen() {
        String userId = mAuth.getCurrentUser().getUid();

        // Get all announcements
        mDatabase.child("announcements")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot announcementSnap : dataSnapshot.getChildren()) {
                            String announcementId = announcementSnap.getKey();
                            // Mark as seen in user's database node
                            mDatabase.child("users").child(userId)
                                    .child("seen_announcements").child(announcementId)
                                    .setValue(ServerValue.TIMESTAMP);
                        }
                        // Reset the announcement icon
                        fabAnnouncements.setImageResource(R.drawable.ic_announcement);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // No action needed
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Show/hide admin menu based on role
        menu.findItem(R.id.action_admin).setVisible(isAdmin);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            // Sign out from Firebase
            mAuth.signOut();

            // Return to login screen
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_admin) {
            // Go to admin dashboard
            startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the screen
        loadLatestAnnouncement();
    }
}