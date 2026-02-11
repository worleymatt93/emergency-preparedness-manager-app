package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;
import java.util.Objects;

public class KitEditActivity extends AppCompatActivity {

    // ------------------- CONSTANTS -------------------

    public static final String EXTRA_KIT_ID = "kitID";

    // ------------------- UI -------------------

    private MaterialToolbar toolbar;
    private TextInputEditText editKitName, editLocation, editNotes, notificationTimeText;
    private MaterialCheckBox kitNotifyCheckbox;
    private View notificationTimeLayout;
    private MaterialButton btnSaveKit;

    // ------------------- NOTIFICATION TIME -------------------

    private Integer selectedHour = null;
    private Integer selectedMinute = null;

    // ------------------- DATA -------------------

    private Repository repository;
    private Kit currentKit;
    private int kitID = -1;

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
        setupNotificationTime();
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

        editKitName = findViewById(R.id.kitNameText);
        editLocation = findViewById(R.id.locationText);
        editNotes = findViewById(R.id.notesText);

        kitNotifyCheckbox = findViewById(R.id.kitNotifyCheckbox);
        notificationTimeLayout = findViewById(R.id.kitNotificationTimeLayout);
        notificationTimeText = findViewById(R.id.kitNotificationTimeText);

        btnSaveKit = findViewById(R.id.btnSaveKit);

        // Hide time picker row until checkbox is checked
        notificationTimeLayout.setVisibility(View.GONE);
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

    private void setupNotificationTime() {
        kitNotifyCheckbox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            notificationTimeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // If they just enabled it and no time was chosen yet, default to 09:00
            if (isChecked && selectedHour == null && selectedMinute == null) {
                selectedHour = 9;
                selectedMinute = 0;
                notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
            }
        }));

        notificationTimeText.setOnClickListener(v -> showTimePicker());
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

            kitNotifyCheckbox.setChecked(kit.isNotificationsEnabled());

            if (kit.isNotificationsEnabled()) {
                notificationTimeLayout.setVisibility(View.VISIBLE);
                selectedHour = kit.getNotifyHour();
                selectedMinute = kit.getNotifyMinute();
                notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
            }
        });
    }

    // ------------------- SAVE -------------------

    private void saveKit() {
        btnSaveKit.setEnabled(false);
        kitNotifyCheckbox.setEnabled(false);

        String name = Objects.requireNonNull(editKitName.getText()).toString().trim();
        String location = Objects.requireNonNull(editLocation.getText()).toString().trim();
        String notes = Objects.requireNonNull(editNotes.getText()).toString().trim();
        boolean notificationsEnabled = kitNotifyCheckbox.isChecked();

        if (name.isEmpty()) {
            showToast(getString(R.string.kit_name_required));
            btnSaveKit.setEnabled(true);
            kitNotifyCheckbox.setEnabled(true);
            return;
        }

        int hour = selectedHour != null ? selectedHour : 9;
        int minute = selectedMinute != null ? selectedMinute : 0;

        if (kitID == -1) {
            // Create new kit, then navigate to items screen
            Kit newKit = new Kit(name, location, notes, notificationsEnabled, hour, minute);

            repository.insert(newKit, newId -> {
                kitID = newId.intValue();
                newKit.setKitID(kitID);
                currentKit = newKit;

                showToast(getString(R.string.kit_added));

                // Navigate to KitItemsActivity
                Intent intent = new Intent(this, KitItemsActivity.class);
                intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, kitID);
                intent.putExtra(KitItemsActivity.EXTRA_KIT_NAME, newKit.getKitName());

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);

                finish();
            });

        } else {
            // Update existing kit
            Kit updated = new Kit(kitID, name, location, notes, notificationsEnabled, hour, minute);

            repository.update(updated, () -> {
                showToast(getString(R.string.kit_updated));
                finish();
            });
        }
    }

    // ------------------- TIME PICKER -------------------

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour != null ? selectedHour : 9)
                .setMinute(selectedMinute != null ? selectedMinute : 0)
                .setTitleText(getString(R.string.select_notification_time))
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();
            notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    // ------------------- TOAST -------------------

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
