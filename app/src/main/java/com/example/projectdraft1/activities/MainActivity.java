package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private TextView txtWelcome;
    private CardView cardProfile, cardNewsroom, cardMessages, cardAgenda;
    private Button btnAdmin;
    private FloatingActionButton fabAnnouncements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Not logged in, go to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
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
            startActivity(new Intent(MainActivity.this, AnnouncementsActivity.class));
        });

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
        });

        // Update welcome message
        updateWelcomeMessage();

        // Check if user is admin
        checkAdminStatus();
    }

    private void initializeViews() {
        txtWelcome = findViewById(R.id.txt_welcome);
        cardProfile = findViewById(R.id.card_profile);
        cardNewsroom = findViewById(R.id.card_newsroom);
        cardMessages = findViewById(R.id.card_messages);
        cardAgenda = findViewById(R.id.card_agenda);
        btnAdmin = findViewById(R.id.btn_admin);
        fabAnnouncements = findViewById(R.id.fab_announcements);
    }

    private void updateWelcomeMessage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                txtWelcome.setText(getString(R.string.welcome_user, displayName));
            }
        }
    }

    private void checkAdminStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() &&
                        Boolean.TRUE.equals(dataSnapshot.child("isAdmin").getValue(Boolean.class))) {
                    // Show admin button
                    btnAdmin.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error checking admin status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}