package com.example.emergencypreparednessmanager.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility class for scheduling and cancelling inexact notifications using AlarmManager.
 * <p>
 * Supports:
 * <ul>
 *   <li>One-time notifications via {@link AlarmManager#setWindow(int, long, long, PendingIntent)},
 *       allowing the system to batch alarms within a window for battery efficiency</li>
 *   <li>Repeating notifications via {@link AlarmManager#setInexactRepeating(int, long, long, PendingIntent)}</li>
 * </ul>
 * Date strings must be in "MM/dd/yyyy" format.
 * Global notification time is stored in SharedPreferences and defaults to 09:00.
 * <p>
 * Compatible with minSdk 26 (Android 8.0) and tested up to API 36 (Android 16).
 */
public class NotificationScheduler {

  //region Constants
  private static final String TAG = "NotificationScheduler";
  private static final String DATE_PATTERN = "MM/dd/yyyy";

  private static final String KEY_HOUR = "notify_hour";
  private static final String KEY_MINUTE = "notify_minute";
  private static final int DEFAULT_HOUR = 9;
  private static final int DEFAULT_MINUTE = 0;

  private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
  private static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;

  private static final long ONE_TIME_WINDOW_MILLIS =
      15L * 60L * 1000L; // 15-minute batching windows

  private static final int PI_FLAGS =
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

  public static final String EXTRA_TYPE = "extra_type";
  public static final String EXTRA_ITEM_ID = "extra_item_id";
  public static final String EXTRA_KIT_ID = "extra_kit_id";
  public static final String EXTRA_DAYS_BEFORE = "extra_days_before";

  public static final String EXTRA_TITLE = "notificationTitle";
  public static final String EXTRA_MESSAGE = "notificationMessage";
  public static final String EXTRA_REQUEST_CODE = "requestCode";

  public static final String TYPE_ITEM_ZERO = "TYPE_ITEM_ZERO";
  public static final String TYPE_ITEM_EXPIRED = "TYPE_ITEM_EXPIRED";
  public static final String TYPE_ITEM_EXPIRES_SOON = "TYPE_ITEM_EXPIRES_SOON";
  public static final String TYPE_KIT_REMINDER = "TYPE_KIT_REMINDER";
  //endregion

  //region Request Code Generation

  /**
   * Generates a unique request code from an ID and type string. Uses hashCode() of concatenated
   * string — collisions are extremely rare in practice.
   *
   * @param id   unique identifier (e.g., kit ID or item ID as string)
   * @param type notification type (e.g., "KIT", "ITEM_ZERO")
   * @return unique integer code for use with PendingIntent
   */
  public static int generateRequestCode(String id, String type) {
    int code = (id + "_" + type).hashCode();

    if (BuildConfig.DEBUG) { // only log in debug builds
      Log.d(TAG, "Generated requestCode=" + code + " for id=" + id + ", type=" + type);
    }
    return code;
  }
  //endregion


  //region Global Notifications enabled
  public static void setNotificationsEnabled(Context context, boolean enabled) {
    context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
        .apply();
  }

  public static boolean areNotificationsEnabled(Context context) {
    return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED);
  }
  //endregion

  //region Global Notification Time
  /**
   * Sets the global notification time (hour and minute) used when no specific time is provided.
   *
   * @param context application context
   * @param hour24  hour of day in 24-hour format (0-23)
   * @param minute  minute of the hour (0-59)
   */
  public static void setGlobalTime(Context context, int hour24, int minute) {
    context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(KEY_HOUR, hour24)
        .putInt(KEY_MINUTE, minute)
        .apply();
  }

  public static int getGlobalHour(Context context) {
    return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_HOUR, DEFAULT_HOUR);
  }

  public static int getGlobalMinute(Context context) {
    return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_MINUTE, DEFAULT_MINUTE);
  }
  //endregion

  //region Schedule (date string)
  /**
   * Schedules a one-time inexact notification for a specific date at the given (or global) time. If
   * the calculated trigger time is in the past, bumps it to the next global time tomorrow.
   *
   * @param context     application context
   * @param receiver    BroadcastReceiver class that handles the alarm
   * @param title       notification title
   * @param message     notification message body
   * @param dateString  date in "MM/dd/yyyy" format
   * @param requestCode unique code identifying this alarm
   * @param hour        optional hour (24h); uses global if null
   * @param minute      optional minute; uses global if null
   * @param itemId      optional item id
   * @param kitId       optional kit id
   * @param daysBefore  optional days-before value (for "expires soon" alarms)
   */
  public static void schedule(
      Context context,
      Class<?> receiver,
      String type,
      String title,
      String message,
      String dateString,
      int requestCode,
      @Nullable Integer hour,
      @Nullable Integer minute,
      @Nullable String itemId,
      @Nullable String kitId,
      @Nullable Integer daysBefore
  ) {
    if (!areNotificationsEnabled(context)) {
      Log.d(TAG, "Notifications disabled. Skipping schedule() for code=" + requestCode);
      return;
    }

    Log.d(TAG, "Scheduling one-time: code=" + requestCode + ", type=" + type + ", date=" + dateString);

    int notifHour = (hour != null) ? hour : getGlobalHour(context);
    int notifMinute = (minute != null) ? minute : getGlobalMinute(context);

    int[] ymd = parseMonthDayYearToYmdUtc(dateString);
    if (ymd == null) {
      Log.e(TAG, "Invalid date format: " + dateString);
      return;
    }

    // Build the trigger time in the user's local timezone, using the chosen calendar date.
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(ymd[0], ymd[1], ymd[2], notifHour, notifMinute, 0);
    cal.set(Calendar.MILLISECOND, 0);

    long triggerAt = cal.getTimeInMillis();

    // If trigger time is already passed, bump to the next global notification time
    if (triggerAt <= System.currentTimeMillis()) {
      triggerAt = nextGlobalTriggerMillis(context);
    }

    scheduleAtMillis(
        context,
        receiver,
        type,
        title,
        message,
        triggerAt,
        requestCode,
        itemId,
        kitId,
        daysBefore
    );
  }
  //endregion

  //region Schedule (millis)
  /**
   * Schedules a one-time inexact notification at the specified trigger time.
   *
   * @param context         application context
   * @param receiver        BroadcastReceiver class
   * @param type            notification type (TYPE_*)
   * @param title           notification title
   * @param message         notification message
   * @param triggerAtMillis time in millis when the alarm should ideally fire
   * @param requestCode     unique alarm identifier
   * @param itemId          optional item id
   * @param kitId           optional kit id
   * @param daysBefore      optional daysBefore
   */
  public static void scheduleAtMillis(
      Context context,
      Class<?> receiver,
      String type,
      String title,
      String message,
      long triggerAtMillis,
      int requestCode,
      @Nullable String itemId,
      @Nullable String kitId,
      @Nullable Integer daysBefore
  ) {
    if (!areNotificationsEnabled(context)) {
      Log.d(TAG, "Notifications disabled. Skipping scheduleAtMillis() for code=" + requestCode);
      return;
    }

    Log.d(TAG, "Scheduling at millis: code=" + requestCode + ", triggers=" + triggerAtMillis);

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (am == null) {
      Log.e(TAG, "AlarmManager unavailable");
      return;
    }

    PendingIntent pi = buildPendingIntent(
        context,
        receiver,
        type,
        title,
        message,
        requestCode,
        itemId,
        kitId,
        daysBefore
    );

    // Clear any previous alarm that has this exact PendingIntent
    am.cancel(pi);

    am.setWindow(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        ONE_TIME_WINDOW_MILLIS,
        pi
    );

    Log.d(TAG, "Scheduled inexact window alarm: code=" + requestCode);
  }
  //endregion

  //region Repeating
  /**
   * Schedules an inexact repeating notification every N days.
   *
   * @param context            application context
   * @param receiver           BroadcastReceiver class
   * @param type               notification type (TYPE_*)
   * @param title              notification title
   * @param message            notification message
   * @param firstTriggerMillis initial trigger time
   * @param intervalDays       repeat interval in days (> 0)
   * @param requestCode        unique alarm identifier
   * @param itemId             optional item id
   * @param kitId              optional kit id
   */
  public static void scheduleRepeatingDays(
      Context context,
      Class<?> receiver,
      String type,
      String title,
      String message,
      long firstTriggerMillis,
      int intervalDays,
      int requestCode,
      @Nullable String itemId,
      @Nullable String kitId
  ) {
    if (!areNotificationsEnabled(context)) {
      Log.d(TAG, "Notifications disabled. Skipping scheduleRepeatingDays()");
      return;
    }

    if (intervalDays <= 0) {
      Log.e(TAG, "Invalid intervalDays: " + intervalDays);
      return;
    }

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    if (am == null) {
      Log.e(TAG, "AlarmManager unavailable");
      return;
    }

    PendingIntent pi = buildPendingIntent(
        context,
        receiver,
        type, title,
        message,
        requestCode,
        itemId,
        kitId,
        null);

    // Clear any previous alarm that has this exact PendingIntent
    am.cancel(pi);

    long intervalMillis = intervalDays * 24L * 60L * 60L * 1000L;

    am.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        firstTriggerMillis,
        intervalMillis,
        pi
    );

    Log.d(TAG, "Scheduled repeating alarm: code=" + requestCode + ", every " + intervalDays + " days");
  }

  /**
   * Schedules a kit reminder using approximate repeating intervals. Falls back to monthly (30 days)
   * for unrecognized frequencies.
   *
   * @param context     application context
   * @param receiver    BroadcastReceiver class
   * @param title       notification title
   * @param message     notification message
   * @param frequency   "MONTHLY", "QUARTERLY", "YEARLY" (case-insensitive)
   * @param requestCode unique alarm identifier
   * @param kitId       kit id
   */
  public static void scheduleKitFrequency(
      Context context,
      Class<?> receiver,
      String title,
      String message,
      String frequency,
      int requestCode,
      @Nullable String kitId
  ) {
    if (!areNotificationsEnabled(context)) {
      Log.d(TAG, "Notifications disabled. Skipping scheduleKitFrequency()");
      return;
    }

    String f = (frequency == null) ? "" : frequency.trim().toUpperCase(Locale.US);

    int intervalDays;

    switch (f) {
      case "QUARTERLY":
        intervalDays = 90;
        break;

      case "YEARLY":
        intervalDays = 365;
        break;

      default:
        intervalDays = 30;
        break;
    }

    long firstTrigger = nextGlobalTriggerMillis(context);

    scheduleRepeatingDays(
        context,
        receiver,
        TYPE_KIT_REMINDER,
        title,
        message,
        firstTrigger,
        intervalDays,
        requestCode,
        null,
        kitId
    );
  }
  //endregion

  //region Date Helpers
  /**
   * Subtracts days from a date string and returns the new date in "MM/dd/yyyy".
   *
   * @param baseDateString date in "MM/dd/yyyy"
   * @param daysBefore     number of days to subtract
   * @return new date string, or null if parsing fails
   */
  @Nullable
  public static String subtractDays(String baseDateString, int daysBefore) {

    int[] ymd = parseMonthDayYearToYmdUtc(baseDateString);
    if (ymd == null) return null;

    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);

    cal.clear();
    cal.set(ymd[0], ymd[1], ymd[2], 0, 0, 0);
    cal.set(Calendar.MILLISECOND, 0
    );
    cal.add(Calendar.DAY_OF_YEAR, -daysBefore);

    SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

    return sdf.format(cal.getTime());
  }

  /**
   * Returns the next occurrence of the global notification time. If today's time has passed,
   * returns tomorrow at that time.
   *
   * @param context application context
   * @return millis timestamp of next global notification time
   */
  public static long nextGlobalTriggerMillis(Context context) {

    int hour = getGlobalHour(context);
    int minute = getGlobalMinute(context);

    Calendar now = Calendar.getInstance();
    Calendar next = (Calendar) now.clone();

    next.set(Calendar.HOUR_OF_DAY, hour);
    next.set(Calendar.MINUTE, minute);
    next.set(Calendar.SECOND, 0);
    next.set(Calendar.MILLISECOND, 0);

    if (!next.after(now)) {
      next.add(Calendar.DAY_OF_YEAR, 1);
    }
    return next.getTimeInMillis();
  }
  //endregion

  //region Cancel
  /**
   * Cancels a previously scheduled notification. Matches the original PendingIntent exactly (same
   * receiver & request code).
   *
   * @param context     application context
   * @param receiver    BroadcastReceiver class
   * @param requestCode the code used when scheduling
   */
  public static void cancel(Context context, Class<?> receiver, int requestCode) {
    Log.d(TAG, "Cancelling notification: code=" + requestCode);

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

    if (am == null) return;

    PendingIntent pi = buildPendingIntent(
        context,
        receiver,
        TYPE_KIT_REMINDER,
        "",
        "",
        requestCode,
        null,
        null,
        null
    );

    am.cancel(pi);
    pi.cancel();
  }
  //endregion

  //region Private Helpers
  private static int[] parseMonthDayYearToYmdUtc(@Nullable String dateString) {

    if (dateString == null) return null;

    try {

      SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
      sdf.setLenient(false);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

      Date d = sdf.parse(dateString);
      if (d == null) return null;

      Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
      utc.setTime(d);

      return new int[]{
          utc.get(Calendar.YEAR),
          utc.get(Calendar.MONTH),
          utc.get(Calendar.DAY_OF_MONTH)
      };

    } catch (Exception e) {
      Log.e(TAG, "Date parse failed:" + dateString, e);
      return null;
    }
  }

  /**
   * Creates a stable action string so each PendingIntent is unambiguous and cannot be "reused"
   * accidentally for a different alarm.
   */
  private static String buildAction(int requestCode) {
    return "com.example.emergencypreparednessmanager.ACTION_NOTIFY_" + requestCode;
  }

  private static PendingIntent buildPendingIntent(
      Context context,
      Class<?> receiver,
      String type,
      String title,
      String message,
      int requestCode,
      @Nullable String itemId,
      @Nullable String kitId,
      @Nullable Integer daysBefore
  ) {

    Intent intent = new Intent(context, receiver);

    // Make the PendingIntent identity unique and stable
    intent.setAction(buildAction(requestCode));

    // Required extras
    intent.putExtra(EXTRA_TITLE, title);
    intent.putExtra(EXTRA_MESSAGE, message);
    intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
    intent.putExtra(EXTRA_TYPE, type);

    // Optional extras
    if (itemId != null) intent.putExtra(EXTRA_ITEM_ID, itemId);
    if (kitId != null) intent.putExtra(EXTRA_KIT_ID, kitId);
    if (daysBefore != null) intent.putExtra(EXTRA_DAYS_BEFORE, daysBefore);

    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PI_FLAGS
    );
  }
  //endregion
}
