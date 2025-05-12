package com.example.projectdraft1.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.projectdraft1.R;
import com.example.projectdraft1.adapters.AgendaAdapter;
import com.example.projectdraft1.models.AgendaItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgendaActivity extends AppCompatActivity {
    private ListView listView;
    private AgendaAdapter adapter;
    private FloatingActionButton fabAddItem;
    private Button btnBack;

    private DatabaseReference agendaRef;
    private DatabaseReference userRef;
    private ValueEventListener agendaListener;
    private List<AgendaItem> agendaList;
    private boolean isAdmin = false;

    private Calendar selectedStartTime = Calendar.getInstance();
    private Calendar selectedEndTime = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        // Initialize Firebase references
        agendaRef = FirebaseDatabase.getInstance().getReference().child("agenda");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

        // Initialize list
        agendaList = new ArrayList<>();

        // Initialize views
        listView = findViewById(R.id.lv_agenda);
        fabAddItem = findViewById(R.id.fab_add_item);
        btnBack = findViewById(R.id.btn_back);

        // Set up ListView
        adapter = new AgendaAdapter(this, agendaList);
        listView.setAdapter(adapter);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Check admin status and set up FAB
        checkAdminStatus();

        // Load agenda items
        loadAgendaItems();
    }

    private void checkAdminStatus() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    isAdmin = Boolean.TRUE.equals(dataSnapshot.child("isAdmin").getValue(Boolean.class));
                    if (isAdmin) {
                        fabAddItem.setVisibility(View.VISIBLE);
                        fabAddItem.setOnClickListener(v -> showAddItemDialog());
                    } else {
                        fabAddItem.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AgendaActivity.this, "Error checking admin status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAgendaItems() {
        agendaListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                agendaList.clear();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    AgendaItem item = itemSnapshot.getValue(AgendaItem.class);
                    if (item != null) {
                        item.setId(itemSnapshot.getKey());
                        agendaList.add(item);
                    }
                }
                // Sort by start time
                agendaList.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AgendaActivity.this, "Error loading agenda",
                        Toast.LENGTH_SHORT).show();
            }
        };

        agendaRef.orderByChild("startTime").addValueEventListener(agendaListener);
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_agenda_item, null);

        EditText edtTitle = dialogView.findViewById(R.id.edt_title);
        EditText edtDescription = dialogView.findViewById(R.id.edt_description);
        EditText edtLocation = dialogView.findViewById(R.id.edt_location);
        TextView txtStartTime = dialogView.findViewById(R.id.txt_start_time);
        TextView txtEndTime = dialogView.findViewById(R.id.txt_end_time);
        Button btnSelectStart = dialogView.findViewById(R.id.btn_select_start);
        Button btnSelectEnd = dialogView.findViewById(R.id.btn_select_end);
        Button btnAdd = dialogView.findViewById(R.id.btn_add);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = builder.setView(dialogView).create();

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

        btnSelectStart.setOnClickListener(v -> {
            showDateTimePicker(selectedStartTime, time -> {
                txtStartTime.setText(displayFormat.format(time.getTime()));
            });
        });

        btnSelectEnd.setOnClickListener(v -> {
            showDateTimePicker(selectedEndTime, time -> {
                txtEndTime.setText(displayFormat.format(time.getTime()));
            });
        });

        btnAdd.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String description = edtDescription.getText().toString().trim();
            String location = edtLocation.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedEndTime.before(selectedStartTime)) {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }

            createAgendaItem(title, description, location,
                    selectedStartTime.getTimeInMillis(),
                    selectedEndTime.getTimeInMillis());
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDateTimePicker(Calendar calendar, OnDateTimeSelectedListener listener) {
        // Date picker
        DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Time picker
                    TimePickerDialog timeDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                listener.onDateTimeSelected(calendar);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false);
                    timeDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private void createAgendaItem(String title, String description, String location,
                                  long startTime, long endTime) {
        String itemId = agendaRef.push().getKey();
        if (itemId == null) return;

        Map<String, Object> item = new HashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("location", location);
        item.put("startTime", startTime);
        item.put("endTime", endTime);
        item.put("createdAt", System.currentTimeMillis());

        agendaRef.child(itemId).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AgendaActivity.this, "Agenda item added!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AgendaActivity.this, "Error adding item",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (agendaListener != null) {
            agendaRef.removeEventListener(agendaListener);
        }
    }

    interface OnDateTimeSelectedListener {
        void onDateTimeSelected(Calendar calendar);
    }
}