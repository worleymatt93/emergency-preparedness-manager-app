package com.example.emergencypreparednessmanager.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import com.example.emergencypreparednessmanager.R;
import com.google.android.material.button.MaterialButton;

/**
 * Main launcher activity for the Emergency Preparedness Manager app.
 * <p>
 * Displays primary navigation buttons to access kits, search, reports, and settings.
 * Requests notification permission on first launch (Android 13+).
 */
public class MainActivity extends BaseActivity {

  //region Constants
  private static final String TAG = "MainActivity";
  private static final String PREFS_FIRST_LAUNCH = "main_prefs";
  private static final String KEY_PROMPTED = "notifications_prompted";
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);

    maybeRequestNotificationPermission();
    bindViews();
  }
  //endregion

  //region Permission Prompt
  /**
   * Prompts for notification permission once on first app launch (Android 13+).
   */
  private void maybeRequestNotificationPermission() {
    if (VERSION.SDK_INT < VERSION_CODES.TIRAMISU) return;

    SharedPreferences prefs = getSharedPreferences(PREFS_FIRST_LAUNCH, MODE_PRIVATE);

    boolean alreadyPrompted = prefs.getBoolean(KEY_PROMPTED, false);

    if (!alreadyPrompted) {
      ensureNotificationPermission();

      prefs.edit()
          .putBoolean(KEY_PROMPTED, true)
          .apply();
    }
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