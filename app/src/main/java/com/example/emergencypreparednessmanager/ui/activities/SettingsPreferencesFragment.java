package com.example.emergencypreparednessmanager.ui.activities;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.util.AppConstants;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import java.util.Locale;

/**
 * Preference-based settings UI: - Theme: System, Light, Dark - Notifications enabled - Notification
 * time (TimePickerDialog) - Household size (1–20) - App version (BuildConfig.VERSION_NAME)
 */
public class SettingsPreferencesFragment extends PreferenceFragmentCompat {

  private int selectedHour;
  private int selectedMinute;

  private int min;
  private int max;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.preferences, rootKey);

    min = getResources().getInteger(R.integer.household_min);
    max = getResources().getInteger(R.integer.household_max);

    setupThemePreference();
    setupNotificationsEnabledPreference();
    setupNotificationTimePreference();
    setupHouseholdSizePreference();
    setupVersionPreference();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    RecyclerView list = getListView();
    list.setClipToPadding(false);

    int pad = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
    list.setPadding(list.getPaddingLeft(), pad, list.getPaddingRight(), pad);
  }

  private void setupThemePreference() {
    ListPreference themePref = findPreference(getString(R.string.pref_key_theme));
    if (themePref == null) {
      return;
    }

    applyTheme(themePref.getValue());

    themePref.setOnPreferenceChangeListener((pref, newValue) -> {
      applyTheme(String.valueOf(newValue));
      requireActivity().recreate();
      return true;
    });
  }

  private void setupNotificationsEnabledPreference() {
    SwitchPreferenceCompat enabledPref =
        findPreference(getString(R.string.pref_key_notifications_enabled));
    if (enabledPref == null) {
      return;
    }

    boolean enabled = NotificationScheduler.areNotificationsEnabled(requireContext());
    enabledPref.setChecked(enabled);

    enabledPref.setOnPreferenceChangeListener((pref, newValue) -> {
      boolean newEnabled = (boolean) newValue;
      NotificationScheduler.setNotificationsEnabled(requireContext(), newEnabled);
      return true;
    });
  }

  private void setupNotificationTimePreference() {
    Preference timePref = findPreference(getString(R.string.pref_key_notification_time));
    if (timePref == null) {
      return;
    }

    selectedHour = NotificationScheduler.getGlobalHour(requireContext());
    selectedMinute = NotificationScheduler.getGlobalMinute(requireContext());
    timePref.setSummary(formatTime(selectedHour, selectedMinute));

    timePref.setOnPreferenceClickListener(pref -> {
      showTimePicker(timePref);
      return true;
    });
  }

  private void showTimePicker(@NonNull Preference timePref) {
    boolean is24Hour = DateFormat.is24HourFormat(requireContext());

    new TimePickerDialog(
        requireContext(),
        (view, hourOfDay, minute) -> {
          selectedHour = hourOfDay;
          selectedMinute = minute;

          NotificationScheduler.setGlobalTime(requireContext(), selectedHour, selectedMinute);
          timePref.setSummary(formatTime(selectedHour, selectedMinute));
        },
        selectedHour,
        selectedMinute,
        is24Hour
    ).show();
  }

  private void setupHouseholdSizePreference() {
    SeekBarPreference householdPref = findPreference(getString(R.string.pref_key_household_size));
    if (householdPref == null) {
      return;
    }

    int existing = getHouseholdSizeFromAppPrefs();
    existing = Math.max(min, Math.min(max, existing));
    householdPref.setValue(existing);

    householdPref.setOnPreferenceChangeListener((pref, newValue) -> {
      int value = (int) newValue;
      value = Math.max(min, Math.min(max, value));
      setHouseholdSizeInAppPrefs(value);
      return true;
    });
  }

  private int getHouseholdSizeFromAppPrefs() {
    SharedPreferences sp = requireContext().getSharedPreferences(AppConstants.PREFS_NAME,
        Context.MODE_PRIVATE);
    return sp.getInt(AppConstants.KEY_HOUSEHOLD_SIZE, AppConstants.DEFAULT_HOUSEHOLD_SIZE);
  }

  private void setHouseholdSizeInAppPrefs(int value) {
    requireContext().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(AppConstants.KEY_HOUSEHOLD_SIZE, value)
        .apply();
  }

  private void setupVersionPreference() {
    Preference versionPref = findPreference(getString(R.string.pref_key_version));
    if (versionPref == null) {
      return;
    }

    try {
      String versionName = requireContext()
          .getPackageManager()
          .getPackageInfo(requireContext().getPackageName(), 0).versionName;

      versionPref.setSummary(versionName);
    } catch (Exception e) {
      versionPref.setSummary("Unknown");
    }
  }

  private void applyTheme(@NonNull String value) {
    if (value.equals(getString(R.string.pref_theme_value_light))) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    } else if (value.equals(getString(R.string.pref_theme_value_dark))) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
  }

  private String formatTime(int hour24, int minute) {
    boolean is24Hours = DateFormat.is24HourFormat(requireContext());
    if (is24Hours) {
      return String.format(Locale.US, "%02d:%02d", hour24, minute);
    }

    int hour12 = hour24 % 12;
    if (hour12 == 0) {
      hour12 = 12;
    }
    String ampm = (hour24 < 12) ? "AM" : "PM";
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, ampm);
  }
}
