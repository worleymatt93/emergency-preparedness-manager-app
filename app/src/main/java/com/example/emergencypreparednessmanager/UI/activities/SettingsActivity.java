package com.example.emergencypreparednessmanager.UI.activities;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS = "epm_prefs";
    private static final String KEY_HOUSEHOLD_SIZE = "household_size";
    private static final int DEFAULT_HOUSEHOLD_SIZE = 1;
    private static final int MINIMUM_HOUSEHOLD_SIZE = 1;
    private static final int MAXIMUM_HOUSEHOLD_SIZE = 20;
    private MaterialToolbar toolbar;
    private TextInputEditText notificationTimeText, householdSizeText;
    private TextInputLayout householdSizeLayout;
    private MaterialButton btnSave;

    private int selectedHour = 9;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupToolbar();
        setupFieldBehaviors();
        loadCurrentValues();
        setupClicks();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        notificationTimeText = findViewById(R.id.notificationTimeText);
        householdSizeLayout = findViewById(R.id.householdSizeLayout);
        householdSizeText = findViewById(R.id.householdSizeText);
        btnSave = findViewById(R.id.btnSaveSettings);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void loadCurrentValues() {
        selectedHour = NotificationScheduler.getGlobalHour(this);
        selectedMinute = NotificationScheduler.getGlobalMinute(this);
        notificationTimeText.setText(formatTime(selectedHour, selectedMinute));

        int householdSize = getHouseholdSize();
        householdSizeText.setText(String.valueOf(householdSize));
    }

    private void setupClicks() {
        notificationTimeText.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false);
            householdSizeLayout.setError(null);

            Integer size = parseHouseholdSize();
            if (size == null) {
                btnSave.setEnabled(true);
                return;
            }

            NotificationScheduler.setGlobalTime(this, selectedHour, selectedMinute);
            setHouseholdSize(size);

            showToast(getString(R.string.settings_saved));
            finish();
        });
    }

    private void showTimePicker() {
        boolean is24Hour = DateFormat.is24HourFormat(this);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
                },
                selectedHour,
                selectedMinute,
                is24Hour
        );

                dialog.show();
    }

    private String formatTime(int hour24, int minute) {
        // Display as user-friendly time, respecting 12/24h mode
        boolean is24Hour = DateFormat.is24HourFormat(this);
        if (is24Hour) {
            return String.format(Locale.US, "%02d:%02d", hour24, minute);
        }

        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        String ampm = (hour24 < 12) ? "AM" : "PM";
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, ampm);
    }

    private void setupFieldBehaviors() {
        householdSizeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                householdSizeLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });
    }

    private int getHouseholdSize() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_HOUSEHOLD_SIZE, DEFAULT_HOUSEHOLD_SIZE);
    }

    private void setHouseholdSize(int value) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_HOUSEHOLD_SIZE, value)
                .apply();
    }

    private Integer parseHouseholdSize() {
        String raw = householdSizeText.getText() != null
                ? householdSizeText.getText().toString().trim()
                : "";

        if (raw.isEmpty()) {
            householdSizeLayout.setError(getString(
                    R.string.error_household_required)
            );
            return null;
        }

        try {
            int value = Integer.parseInt(raw);

            if (value < MINIMUM_HOUSEHOLD_SIZE) {
                householdSizeLayout.setError(getString(
                        R.string.error_household_min_dynamic, MINIMUM_HOUSEHOLD_SIZE)
                );
                return null;
            }

            if (value > MAXIMUM_HOUSEHOLD_SIZE) {
                householdSizeLayout.setError(getString(
                        R.string.error_household_max_dynamic, MAXIMUM_HOUSEHOLD_SIZE)
                );
                return null;
            }

            return value;
        } catch (NumberFormatException e) {
            householdSizeLayout.setError(getString(
                    R.string.error_household_number)
            );
            return null;
        }
    }


    /**
     * Displays a short Toast message.
     * Use this for simple non-interactive messages.
     */
    private void showToast(String message) {
        if (message.length() >= 30) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
