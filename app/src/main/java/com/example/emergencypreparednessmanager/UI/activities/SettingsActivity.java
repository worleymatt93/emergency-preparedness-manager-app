package com.example.emergencypreparednessmanager.UI.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText notificationTimeText;
    private MaterialButton btnSave;

    private int selectedHour = 9;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupToolbar();
        loadCurrentValues();
        setupClicks();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        notificationTimeText = findViewById(R.id.notificationTimeText);
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
    }

    private void setupClicks() {
        notificationTimeText.setOnClickListener(v -> showTimePicker());

        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false);

            NotificationScheduler.setGlobalTime(this, selectedHour, selectedMinute);

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
