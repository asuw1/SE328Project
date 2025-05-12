package com.example.projectdraft1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdraft1.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtEmail;
    private EditText edtName;
    private Button btnUpdate, btnLogout, btnBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No user logged in, go back to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        userId = currentUser.getUid();

        // Initialize views
        txtEmail = findViewById(R.id.txt_email);
        edtName = findViewById(R.id.edt_name);
        btnUpdate = findViewById(R.id.btn_update);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_back);

        // Load user data
        loadUserProfile();

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> updateProfile());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        // Show email from Firebase Auth
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            txtEmail.setText(user.getEmail());
        }

        // Load profile data from database
        mDatabase.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);

                            if (name != null) {
                                edtName.setText(name);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ProfileActivity.this,
                                "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateProfile() {
        String name = edtName.getText().toString().trim();

        if (name.isEmpty()) {
            edtName.setError("Name cannot be empty");
            return;
        }

        // Disable button during update
        btnUpdate.setEnabled(false);

        // Update display name in Firebase Auth
        FirebaseUser user = mAuth.getCurrentUser();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update database
                        updateDatabase(name);
                    } else {
                        btnUpdate.setEnabled(true);
                        Toast.makeText(ProfileActivity.this,
                                "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDatabase(String name) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);

        mDatabase.child("users").child(userId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    btnUpdate.setEnabled(true);
                    Toast.makeText(ProfileActivity.this,
                            "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnUpdate.setEnabled(true);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}