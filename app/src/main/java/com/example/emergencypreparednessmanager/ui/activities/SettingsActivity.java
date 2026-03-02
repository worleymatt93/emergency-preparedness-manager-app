package com.example.emergencypreparednessmanager.ui.activities;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.emergencypreparednessmanager.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Hosts the settings preferences UI. Uses a PreferenceFragmentCompat for settings content.
 */
public class SettingsActivity extends AppCompatActivity {

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_settings);

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
  }
  //endregion
}