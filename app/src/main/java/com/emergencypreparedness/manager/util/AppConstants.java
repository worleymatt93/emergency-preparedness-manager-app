package com.emergencypreparedness.manager.util;

/**
 * Application-wide constants (magic numbers, timeouts, thresholds, etc.).
 * <p>
 * Only values that are reused, configurable, or represent business rules belong here. Pure UI
 * strings belong in strings.xml.
 */
public final class AppConstants {

  /**
   * Delay before firing zero-quantity alert (allows user undo window)
   */
  public static final long ZERO_QUANTITY_ALERT_DELAY_MS = 15_000L;

  //region Time & Delays (milliseconds)
  /**
   * Days before expiration for "Expiring Soon" warning
   */
  public static final int DAYS_BEFORE_EXPIRATION_FOR_WARNING = 30;
  //endregion

  //region Expiration & Reminders
  //region Water Readiness Targets (days)
  public static final int WATER_MIN_DAYS = 3;
  //endregion
  public static final int WATER_REC_DAYS = 14;
  public static final int WATER_PREP_DAYS = 30;
  //region Household Size
  public static final int DEFAULT_HOUSEHOLD_SIZE = 1;
  //endregion
  //region SharedPreferences
  public static final String PREFS_NAME = "epm_prefs";
  public static final String NOTIFICATION_CHANNEL_ALERTS = "epm_alerts";
  //endregion
  public static final String KEY_HOUSEHOLD_SIZE = "household_size";
  //region Export / Share
  public static final String REPORT_FILE_PREFIX = "EmergencyPreparednessReport_";
  //endregion
  public static final String MIME_TYPE_CSV = "text/csv";
  public static final String REPORT_SUMMARY_CLIPBOARD_LABEL = "Emergency Preparedness Report";
  private AppConstants() {
    throw new AssertionError("No instances allowed");
  }
  //endregion
}