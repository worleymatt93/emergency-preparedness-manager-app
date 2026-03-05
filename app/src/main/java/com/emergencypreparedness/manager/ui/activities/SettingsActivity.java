package com.emergencypreparedness.manager.ui.activities;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.emergencypreparedness.manager.R;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Objects;

/**
 * Hosts the settings preferences UI. Uses a PreferenceFragmentCompat for settings content.
 */
public class SettingsActivity extends AppCompatActivity {

  //region Constants
  private MaterialToolbar toolbar;
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_settings);

    EdgeToEdge.enable(this);

    // Bind views
    toolbar = findViewById(R.id.toolbar);

    setupToolbar();
    setupInsets();

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.settings_container, new SettingsPreferencesFragment())
        .commit();
  }

  private void setupToolbar() {
    setSupportActionBar(toolbar);
    if (toolbar != null) {
      toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }
  }

  private void setupInsets() {
    // Toolbar: top clearance for status bar
    if (toolbar != null) {
      ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            v.getPaddingBottom()
        );

        toolbar.setTitleCentered(true);

        return insets;
      });
    }
  }
}
