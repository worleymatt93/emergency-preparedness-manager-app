package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.Objects;

public class KitEditActivity extends AppCompatActivity {

    // ------------------- CONSTANTS -------------------

    public static final String EXTRA_KIT_ID = "kitID";

    // ------------------- UI -------------------

    private MaterialToolbar toolbar;

    private TextInputLayout kitNameLayout;
    private TextInputEditText editKitName;

    private TextInputEditText editLocation, editNotes;

    private MaterialCheckBox kitNotifyCheckbox;

    private TextInputLayout kitFrequencyLayout;
    private MaterialAutoCompleteTextView frequencyDropdown;

    private MaterialButton btnSaveKit;

    // ------------------- DATA -------------------

    private Repository repository;
    private Kit currentKit;
    private int kitID = -1;

    // Stored as the DB-friendly value: "MONTHLY", "QUARTERLY", "YEARLY"
    private String selectedFrequency = null;

    // ------------------- LIFECYCLE -------------------

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
        setupSaveButton();

        if (kitID != -1) {
            loadKit();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_KIT_ID, kitID);
        super.onSaveInstanceState(outState);
    }

    // ------------------- SETUP -------------------

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

        // Hidden until checkbox checked
        kitFrequencyLayout.setVisibility(View.GONE);
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

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        // Enables the ActionBar back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Make the arrow behave like system back
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupFrequencyDropdown() {
        // These are the user-visible labels in the dropdown
        String[] display = new String[]{
                getString(R.string.frequency_monthly),
                getString(R.string.frequency_quarterly),
                getString(R.string.frequency_yearly)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                display
        );

        frequencyDropdown.setAdapter(adapter);

        // Force dropdown on tap
        frequencyDropdown.setOnClickListener(v -> frequencyDropdown.showDropDown());

        frequencyDropdown.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) selectedFrequency = "MONTHLY";
            else if (position == 1) selectedFrequency = "QUARTERLY";
            else if (position == 2) selectedFrequency = "YEARLY";
        });
    }

    private void setupNotificationsSection() {
        kitNotifyCheckbox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            kitFrequencyLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Clear any prior error
            kitFrequencyLayout.setError(null);

            // Do not autofill. Force user selection when enabled
            selectedFrequency = null;
            frequencyDropdown.setText("", false);
        }));
    }

    private void setupFieldBehaviors() {
        editKitName.addTextChangedListener(clearErrorWatcher(kitNameLayout));

        frequencyDropdown.addTextChangedListener(clearErrorWatcher(kitFrequencyLayout));
    }

    private TextWatcher clearErrorWatcher(TextInputLayout layout) {
        return new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        };
    }

    private void setupSaveButton() {
        btnSaveKit.setOnClickListener(v -> saveKit());
    }

    // ------------------- LOAD -------------------

    private void loadKit() {
        repository.getKitById(kitID, kit -> {
            if (kit == null) return;

            currentKit = kit;

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

                // Only show the saved value
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
        if (freq == null) return;

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

    // ------------------- SAVE -------------------

    private void saveKit() {
        btnSaveKit.setEnabled(false);
        kitNotifyCheckbox.setEnabled(false);

        // Clear inline errors
        kitNameLayout.setError(null);
        kitFrequencyLayout.setError(null);

        String name = Objects.requireNonNull(editKitName.getText()).toString().trim();
        String location = Objects.requireNonNull(editLocation.getText()).toString().trim();
        String notes = Objects.requireNonNull(editNotes.getText()).toString().trim();

        boolean notificationsEnabled = kitNotifyCheckbox.isChecked();

        if (name.isEmpty()) {
            kitNameLayout.setError(getString(R.string.kit_name_required));
            btnSaveKit.setEnabled(true);
            kitNotifyCheckbox.setEnabled(true);
            return;
        }

        if (notificationsEnabled && (selectedFrequency == null || selectedFrequency.trim().isEmpty())) {
            kitFrequencyLayout.setError(getString(R.string.frequency_required));
            btnSaveKit.setEnabled(true);
            kitNotifyCheckbox.setEnabled(true);
            return;
        }

        final String freqToSave = notificationsEnabled ? selectedFrequency : null;

        if (kitID == -1) {
            // Create new kit, then navigate to items screen
            Kit newKit = new Kit(name, location, notes, notificationsEnabled, freqToSave);

            repository.insert(newKit, newId -> {
                kitID = newId.intValue();
                newKit.setKitID(kitID);
                currentKit = newKit;

                applyKitNotificationSchedule(newKit);

                showToast(getString(R.string.kit_added));

                // Navigate to KitItemsActivity
                Intent intent = new Intent(this, KitItemsActivity.class);
                intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, kitID);
                intent.putExtra(KitItemsActivity.EXTRA_KIT_NAME, newKit.getKitName());
                startActivity(intent);
                finish();
            });

        } else {
            // Update existing kit
            Kit updated = new Kit(kitID, name, location, notes, notificationsEnabled, freqToSave);

            repository.update(updated, () -> {
                currentKit = updated;
                applyKitNotificationSchedule(updated);
                showToast(getString(R.string.kit_updated));
                finish();
            });
        }
    }

    private void applyKitNotificationSchedule(Kit kit) {
        int requestCode = NotificationScheduler.generateRequestCode(String.valueOf(kit.getKitID()), "KIT");

        // Always cancel first to avoid duplicate PendingIntents when toggling frequency
        NotificationScheduler.cancel(this, AlertReceiver.class, requestCode);

        if (!kit.isNotificationsEnabled()) return;

        String freq = kit.getNotificationFrequency();
        if (freq == null || freq.trim().isEmpty()) return;

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



    // ------------------- TOAST -------------------

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
