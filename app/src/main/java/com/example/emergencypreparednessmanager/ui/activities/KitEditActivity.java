package com.example.emergencypreparednessmanager.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.ui.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Locale;
import java.util.Objects;

/**
 * Activity for creating or editing a kit.
 * <p>
 * Handles kit name, location, notes, notification toggle, and frequency selection.
 * Schedules/cancels notifications on save.
 */
public class KitEditActivity extends BaseActivity {

  //region Constants
  public static final String EXTRA_KIT_ID = "kitID";
  public static final String TAG = "KitEditActivity";

  //region Fields
  private MaterialToolbar toolbar;

  private TextInputLayout kitNameLayout;
  private TextInputEditText editKitName;
  private TextInputEditText editLocation;
  private TextInputEditText editNotes;

  private MaterialCheckBox kitNotifyCheckbox;
  private TextInputLayout kitFrequencyLayout;
  private MaterialAutoCompleteTextView frequencyDropdown;

  private MaterialButton btnSaveKit;
  private MaterialButton btnDeleteKit;

  private Repository repository;
  private int kitID = -1;
  private String selectedFrequency;
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_kit_edit);

    repository = new Repository(getApplication());

    kitID = savedInstanceState != null
        ? savedInstanceState.getInt(EXTRA_KIT_ID, -1)
        : getIntent().getIntExtra(EXTRA_KIT_ID, -1);

    bindViews();
    setupInsets();
    setupToolbar();
    setupFrequencyDropdown();
    setupNotificationsSection();
    setupFieldBehaviors();
    setupButtons();

    if (kitID != -1) {
      loadKit();
    }
  }

  @Override
  protected void onSaveInstanceState(@NonNull Bundle outState) {
    outState.putInt(EXTRA_KIT_ID, kitID);
    super.onSaveInstanceState(outState);
  }
  //endregion

  // region Setup
  private void bindViews() {
    toolbar = findViewById(R.id.toolbar);

    kitNameLayout = findViewById(R.id.kitNameLayout);
    editKitName = findViewById(R.id.kitNameText);

    editLocation = findViewById(R.id.locationText);
    editNotes = findViewById(R.id.notesText);

    kitNotifyCheckbox = findViewById(R.id.kitNotifyCheckbox);

    kitFrequencyLayout = findViewById(R.id.kitFrequencyLayout);
    frequencyDropdown = findViewById(R.id.kitFrequencyDropdown);

    btnSaveKit = findViewById(R.id.btnSaveKit);
    btnDeleteKit = findViewById(R.id.btnDeleteKit);

    // Hide frequency section until checkbox is checked
    kitFrequencyLayout.setVisibility(View.GONE);

    // Hide delete button on create screen
    btnDeleteKit.setVisibility(kitID != -1 ? View.VISIBLE : View.GONE);
  }

  private void setupInsets() {
    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    if (toolbar == null) {
      return;
    }

    ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
      WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
          Objects.requireNonNull(insets.toWindowInsets()));
      androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
          WindowInsetsCompat.Type.systemBars());

      // Apply insets — background under status bar, content cleared
      toolbar.setPadding(
          systemBars.left,
          systemBars.top,
          systemBars.right,
          toolbar.getPaddingBottom()
      );

      toolbar.setTitleCentered(true);

      return insets;
    });

    // Bottom padding for scrollView (unchanged)
    NestedScrollView scrollView = findViewById(R.id.scrollView);
    if (scrollView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            v.getPaddingLeft(),
            v.getPaddingTop(),
            v.getPaddingRight(),
            systemBars.bottom
        );

        return insets;
      });
    }
  }

  private void setupToolbar() {
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
  }

  private void setupFrequencyDropdown() {
    String[] displayLabels = new String[]{
        getString(R.string.frequency_monthly),
        getString(R.string.frequency_quarterly),
        getString(R.string.frequency_yearly)
    };

    ArrayAdapter<String> adapter = new ArrayAdapter<>(
        this,
        android.R.layout.simple_list_item_1,
        displayLabels
    );

    frequencyDropdown.setAdapter(adapter);
    frequencyDropdown.setOnClickListener(v -> frequencyDropdown.showDropDown());

    frequencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
      if (position == 0) {
        selectedFrequency = "MONTHLY";
      } else if (position == 1) {
        selectedFrequency = "QUARTERLY";
      } else if (position == 2) {
        selectedFrequency = "YEARLY";
      }
    });
    kitFrequencyLayout.setError(null);
  }

  private void setupNotificationsSection() {
    kitNotifyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
      kitFrequencyLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      kitFrequencyLayout.setError(null);

      // DForce re-selection when re-enabled
      if (isChecked) {
        selectedFrequency = null;
        frequencyDropdown.setText("", false);
      }
    });
  }

  private void setupFieldBehaviors() {
    editKitName.addTextChangedListener(clearErrorWatcher(kitNameLayout));
    frequencyDropdown.addTextChangedListener(clearErrorWatcher(kitFrequencyLayout));
  }

  private TextWatcher clearErrorWatcher(TextInputLayout layout) {
    return new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        layout.setError(null);
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    };
  }

  private void setupButtons() {
    btnSaveKit.setOnClickListener(v -> saveKit());
    btnDeleteKit.setOnClickListener(v -> confirmDeleteKit());
  }
  //endregion

  //region Load
  private void loadKit() {
    repository.getKitById(kitID, kit -> {
      if (kit == null) {
        return;
      }

      editKitName.setText(kit.getKitName());
      editLocation.setText(kit.getLocation());
      editNotes.setText(kit.getNotes());

      String freq = kit.getNotificationFrequency();
      selectedFrequency = (freq != null && !freq.trim().isEmpty())
          ? freq.trim().toUpperCase(Locale.US)
          : null;

      kitNotifyCheckbox.setChecked(kit.isNotificationsEnabled());

      if (kit.isNotificationsEnabled()) {
        kitFrequencyLayout.setVisibility(View.VISIBLE);
        if (selectedFrequency != null) {
          syncFrequencyDropdown(selectedFrequency);
        } else {
          frequencyDropdown.setText("", false);
        }
      } else {
        kitFrequencyLayout.setVisibility(View.GONE);
        frequencyDropdown.setText("", false);
      }
    });
  }

  private void syncFrequencyDropdown(String freq) {
    if (freq == null) {
      return;
    }

    switch (freq) {
      case "MONTHLY":
        frequencyDropdown.setText(getString(R.string.frequency_monthly), false);
        break;
      case "QUARTERLY":
        frequencyDropdown.setText(getString(R.string.frequency_quarterly), false);
        break;
      case "YEARLY":
        frequencyDropdown.setText(getString(R.string.frequency_yearly), false);
        break;
      default:
        // Unknown value: keep blank and force re-select
        selectedFrequency = null;
        frequencyDropdown.setText("", false);
        break;
    }
  }
  //endregion

  //region Save / Delete
  private void saveKit() {
    btnSaveKit.setEnabled(false);
    kitNotifyCheckbox.setEnabled(false);

    kitNameLayout.setError(null);
    kitFrequencyLayout.setError(null);

    String name = editKitName.getText() != null ? editKitName.getText().toString().trim() : "";
    String location =
        editLocation.getText() != null ? editLocation.getText().toString().trim() : "";
    String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";

    boolean notificationsEnabled = kitNotifyCheckbox.isChecked();

    if (name.isEmpty()) {
      kitNameLayout.setError(getString(R.string.kit_name_required));
      restoreSaveButton();
      return;
    }

    if (notificationsEnabled && (selectedFrequency == null || selectedFrequency.trim().isEmpty())) {
      kitFrequencyLayout.setError(getString(R.string.frequency_required));
      restoreSaveButton();
      return;
    }

    String freqToSave = notificationsEnabled ? selectedFrequency : null;

    Kit kit = kitID == -1
        ? new Kit(name, location, notes, notificationsEnabled, freqToSave)
        : new Kit(kitID, name, location, notes, notificationsEnabled, freqToSave);

    if (kitID == -1) {
      repository.insertKit(kit, newId -> {
        kitID = newId.intValue();
        kit.setKitID(kitID);

        applyKitNotificationSchedule(kit);

        showToast(getString(R.string.kit_added));

        Intent intent = new Intent(this, KitItemsActivity.class);
        intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, kitID);
        intent.putExtra(KitItemsActivity.EXTRA_KIT_NAME, kit.getKitName());
        startActivity(intent);
        finish();
      });
    } else {
      // Update existing
      repository.updateKit(kit, () -> {
        applyKitNotificationSchedule(kit);
        showToast(getString(R.string.kit_updated));
        finish();
      });
    }
  }

  private void restoreSaveButton() {
    btnSaveKit.setEnabled(true);
    kitNotifyCheckbox.setEnabled(true);
  }

  private void confirmDeleteKit() {
    showDeleteConfirmation(getString(R.string.delete_kit), "kit", this::performDeleteKit);
  }

  private void performDeleteKit() {
    // Minimal Kit object for delete (only ID needed)
    Kit kitToDelete = new Kit();
    kitToDelete.setKitID(kitID);

    // Cancel notifications first
    int requestCode = NotificationScheduler.generateRequestCode(
        String.valueOf(kitID), "KIT");
    NotificationScheduler.cancel(this, AlertReceiver.class, requestCode);

    repository.deleteKit(kitToDelete, () -> {
      showToast(getString(R.string.kit_deleted));

      Intent intent = new Intent(this, KitListActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intent);
      finish();
    });
  }


  private void applyKitNotificationSchedule(Kit kit) {
    int requestCode = NotificationScheduler.generateRequestCode(
        String.valueOf(kit.getKitID()), "KIT");

    // Always cancel first to avoid duplicates on update/toggle
    NotificationScheduler.cancel(this, AlertReceiver.class, requestCode);

    if (!kit.isNotificationsEnabled()) {
      return;
    }

    String freq = kit.getNotificationFrequency();
    if (freq == null || freq.trim().isEmpty()) {
      return;
    }

    String title = getString(R.string.kit_notification_title, kit.getKitName());
    String message = getString(R.string.kit_notification_message, kit.getKitName());

    NotificationScheduler.scheduleKitFrequency(
        this,
        AlertReceiver.class,
        title,
        message,
        freq,
        requestCode
    );
  }
  //endregion

  //region Helpers
  private void showDeleteConfirmation(String title, String type, Runnable onConfirm) {
    new MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(getString(R.string.permanent_delete_warning, type) + "\n" +
            getString(R.string.cannot_be_undone))
        .setPositiveButton(
            getString(R.string.delete), (dialog, which) -> onConfirm.run()
        )
        .setNegativeButton(android.R.string.cancel, null)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .show();
  }

  private void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
        .show();
  }
  //endregion
}
