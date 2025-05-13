package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.projectdraft1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {
    private TextView txtTotalUsers;
    private CardView cardAnnouncements, cardAgenda;
    private Button btnBack;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Verify admin status
        verifyAdminAccess();

        // Initialize Firebase
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Initialize views
        txtTotalUsers = findViewById(R.id.txt_total_users);
        cardAnnouncements = findViewById(R.id.card_announcements);
        cardAgenda = findViewById(R.id.card_agenda);
        btnBack = findViewById(R.id.btn_back);

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());

        cardAnnouncements.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AnnouncementsActivity.class));
        });

        cardAgenda.setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AgendaActivity.class));
        });

        // Load statistics
        loadUserCount();
    }

    private void verifyAdminAccess() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(currentUserId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() ||
                        !Boolean.TRUE.equals(dataSnapshot.child("isAdmin").getValue(Boolean.class))) {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Error verifying admin access", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadUserCount() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long userCount = dataSnapshot.getChildrenCount();
                txtTotalUsers.setText(String.valueOf(userCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                txtTotalUsers.setText("Error");
                Toast.makeText(AdminDashboardActivity.this,
                        "Error loading user count", Toast.LENGTH_SHORT).show();
            }
        });
    }
}