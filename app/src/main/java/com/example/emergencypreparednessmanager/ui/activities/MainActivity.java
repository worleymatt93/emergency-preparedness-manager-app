package com.example.emergencypreparednessmanager.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.emergencypreparednessmanager.R;
import com.google.android.material.button.MaterialButton;

/**
 * Main launcher activity for the Emergency Preparedness Manager app.
 * <p>
 * Displays primary navigation buttons to access kits, search, reports, and settings. Uses
 * edge-to-edge display.
 */
public class MainActivity extends AppCompatActivity {

  //region Constants
  private static final String TAG = "MainActivity";
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Enable Edge-to-Edge display (content draws behind status/navigation bars)
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    bindViews();
  }
  //endregion

  //region Setup

  /**
   * Binds buttons to their IDs and sets click listeners for navigation.
   */
  private void bindViews() {
    MaterialButton btnKits = findViewById(R.id.btnKits);
    MaterialButton btnSearch = findViewById(R.id.btnSearch);
    MaterialButton btnReports = findViewById(R.id.btnReports);
    MaterialButton btnSettings = findViewById(R.id.btnSettings);

    btnKits.setOnClickListener(v -> startActivity(new Intent(this, KitListActivity.class)));
    btnSearch.setOnClickListener(
        v -> startActivity(new Intent(this, SearchSuppliesActivity.class)));
    btnReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
    btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
  }
}