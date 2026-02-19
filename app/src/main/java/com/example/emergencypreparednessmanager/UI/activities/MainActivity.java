package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emergencypreparednessmanager.R;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge layout for modern Android devices
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bindViews();
        setupInsets();

    }

    // ------------------- SETUP -------------------

    /**
     * Binds UI components to their variables and sets click listeners.
     * Main actions navigate to Kits, Search, and Reports.
     */
    private void bindViews() {
        MaterialButton btnKits = findViewById(R.id.btnKits);
        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        MaterialButton btnReports = findViewById(R.id.btnReports);

        btnKits.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KitListActivity.class);
            startActivity(intent);
        });

        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchSuppliesActivity.class);
            startActivity(intent);
        });

        /* TODO Create ReportsActivity
        btnReports.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
            startActivity(intent);
        });

         */
    }

    /**
     * Applies padding for system bars (status/navigation) to ensure content is not overlapped.
     */
    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}