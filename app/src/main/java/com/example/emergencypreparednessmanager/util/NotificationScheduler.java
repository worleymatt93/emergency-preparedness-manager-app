package com.example.emergencypreparednessmanager.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for scheduling and cancelling inexact notifications using AlarmManager.
 * <p>
 * Supports:
 * <ul>
 *   <li>One-time notifications via {@link AlarmManager#setWindow(int, long, long, PendingIntent)}
 *       (available since API 19), allowing the system to batch alarms within a window for battery efficiency</li>
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
  private static final boolean DEBUG = true;
  private static final String DATE_PATTERN = "MM/dd/yyyy";

  private static final String KEY_HOUR = "notify_hour";
  private static final String KEY_MINUTE = "notify_minute";
  private static final int DEFAULT_HOUR = 9;
  private static final int DEFAULT_MINUTE = 0;

  private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
  private static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;

  private static final long ONE_TIME_WINDOW_MILLIS =
      15L * 60L * 1000L; // 15-minute batching windows
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
    log("Generated requestCode=" + code + " for id=" + id + ", type=" + type);
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

  //region Global Notification Time (SharedPreferences)

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

  //region Scheduling - Date String

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
   */
  public static void schedule(Context context,
      Class<?> receiver,
      String title,
      String message,
      String dateString,
      int requestCode,
      @Nullable Integer hour,
      @Nullable Integer minute) {

    if (!areNotificationsEnabled(context)) {
      log("Notifications disabled. Skipping schedule() for code=" + requestCode);
      return;
    }

    log("Scheduling notification: code=" + requestCode + ", title=" + title +
        "date=" + dateString + ", hour=" + hour + ",minute=" + minute);

    int notifHour = (hour != null) ? hour : getGlobalHour(context);
    int notifMinute = (minute != null) ? minute : getGlobalMinute(context);

    Date date = parseDate(dateString);
    if (date == null) {
      log("Invalid date format: " + dateString);
      return;
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, notifHour);
    cal.set(Calendar.MINUTE, notifMinute);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    long triggerAt = cal.getTimeInMillis();

    // If trigger time already passed, trigger in 2 seconds
    if (triggerAt <= System.currentTimeMillis()) {
      triggerAt = System.currentTimeMillis() + 2000; // 2 seconds from now
    }

    scheduleAtMillis(context, receiver, title, message, triggerAt, requestCode);
  }
  //endregion

  //region Scheduling - Milliseconds

  /**
   * Schedules a one-time inexact notification at the specified trigger time.
   *
   * @param context         application context
   * @param receiver        BroadcastReceiver class
   * @param title           notification title
   * @param message         notification message
   * @param triggerAtMillis time in millis when the alarm should ideally fire
   * @param requestCode     unique alarm identifier
   */
  public static void scheduleAtMillis(Context context,
      Class<?> receiver,
      String title,
      String message,
      long triggerAtMillis,
      int requestCode) {

    if (!areNotificationsEnabled(context)) {
      log("Notifications disabled. Skipping schedule() for code=" + requestCode);
      return;
    }

    log("Scheduling at millis: code=" + requestCode + ", triggers=" + triggerAtMillis);

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (am == null) {
      log("AlarmManager unavailable");
      return;
    }

    PendingIntent pi = buildPendingIntent(context, receiver, title, message, requestCode);

    am.setWindow(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        ONE_TIME_WINDOW_MILLIS,
        pi
    );

    log("Scheduled inexact window alarm: code=" + requestCode);
  }
  //endregion

  //region Scheduling - Repeating

  /**
   * Schedules an inexact repeating notification every N days.
   *
   * @param context            application context
   * @param receiver           BroadcastReceiver class
   * @param title              notification title
   * @param message            notification message
   * @param firstTriggerMillis initial trigger time
   * @param intervalDays       repeat interval in days (> 0)
   * @param requestCode        unique alarm identifier
   */
  public static void scheduleRepeatingDays(Context context,
      Class<?> receiver,
      String title,
      String message,
      long firstTriggerMillis,
      int intervalDays,
      int requestCode) {

    if (!areNotificationsEnabled(context)) {
      log("Notifications disabled. Skipping scheduleRepeatingDays() for code=" + requestCode);
      return;
    }

    if (intervalDays <= 0) {
      log("Invalid intervalDays: " + intervalDays);
      return;
    }

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (am == null) {
      return;
    }

    PendingIntent pi = buildPendingIntent(context, receiver, title, message, requestCode);

    long intervalMillis = intervalDays * 24L * 60L * 60L * 1000L;

    am.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        firstTriggerMillis,
        intervalMillis,
        pi
    );

    log("Scheduled repeating alarm: code=" + requestCode + ", every " + intervalDays + " days");
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
   */
  public static void scheduleKitFrequency(Context context,
      Class<?> receiver,
      String title,
      String message,
      String frequency,
      int requestCode) {

    if (!areNotificationsEnabled(context)) {
      log("Notifications disabled. Skipping scheduleKitFrequency() for code=" + requestCode);
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
    scheduleRepeatingDays(context, receiver, title, message, firstTrigger, intervalDays,
        requestCode);
  }
  //endregion

  //region Helpers - Date & Time Utilities

  /**
   * Subtracts days from a date string and returns the new date in "MM/dd/yyyy".
   *
   * @param baseDateString date in "MM/dd/yyyy"
   * @param daysBefore     number of days to subtract
   * @return new date string, or null if parsing fails
   */
  @Nullable
  public static String subtractDays(String baseDateString, int daysBefore) {
    Date base = parseDate(baseDateString);
    if (base == null) {
      return null;
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(base);
    cal.add(Calendar.DAY_OF_YEAR, -daysBefore);

    return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(cal.getTime());
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
    log("Cancelling notification: code=" + requestCode);

    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (am == null) {
      return;
    }

    PendingIntent pi = PendingIntent.getBroadcast(
        context,
        requestCode,
        new Intent(context, receiver),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    am.cancel(pi);
    pi.cancel();
    log("Cancelled alarm code=" + requestCode);
  }
  //endregion

  //region Private Helpers
  private static void log(String msg) {
    if (DEBUG) {
      Log.d(TAG, msg);
    }
  }

  @Nullable
  private static Date parseDate(String dateString) {
    try {
      return new SimpleDateFormat(DATE_PATTERN, Locale.US).parse(dateString);
    } catch (Exception e) {
      log("Date parse failed: " + dateString + " → " + e.getMessage());
      return null;
    }
  }

  private static PendingIntent buildPendingIntent(Context context,
      Class<?> receiver,
      String title,
      String message,
      int requestCode) {
    Intent intent = new Intent(context, receiver);
    intent.putExtra("notificationTitle", title);
    intent.putExtra("notificationMessage", message);
    intent.putExtra("requestCode", requestCode);

    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
  }
  //endregion
}
