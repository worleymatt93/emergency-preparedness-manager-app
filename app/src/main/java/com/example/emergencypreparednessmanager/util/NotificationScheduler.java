package com.example.emergencypreparednessmanager.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for scheduling and cancelling INEXACT notifications.
 *
 * One-time notifications:
 * - Uses AlarmManager#setWindow(...) so the system can batch alarms.
 *
 * Repeating notifications:
 * - Uses AlarmManager#setInexactRepeating(...)
 *
 * Date strings are expected in "MM/dd/yyyy".
 */
public class NotificationScheduler {

    // ------------------- CONSTANTS -------------------

    private static final String TAG = "NotificationScheduler";
    private static final boolean DEBUG = true;
    private static final String DATE_PATTERN = "MM/dd/yyyy";

    // SharedPreferences for global notification time
    private static final String PREFS = "epm_prefs";
    private static final String KEY_HOUR = "notify_hour";
    private static final String KEY_MINUTE = "notify_minute";
    private static final int DEFAULT_HOUR = 9;
    private static final int DEFAULT_MINUTE = 0;

    // Window size for inexact "one-time" alarms (system can fire anytime in this window)
    // 15 minutes is a reasonable batching window.
    private static final long ONE_TIME_WINDOW_MILLIS = 15L * 60L * 1000L;

    // ------------------- LOGGING -------------------

    /**
     * Logs a debug message if debugging is enabled.
     *
     * @param msg   the message to log
     */
    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    // ------------------- REQUEST CODE -------------------

    /**
     * Generates a unique request code for a notification based on an ID and type.
     *
     * @param id        unique identifier (e.g., vacation or excursion ID)
     * @param type      type of notification (e.g., "START", "END", "EXCURSION")
     * @return Unique integer hash code for PendingIntent
     */
    public static int generateRequestCode(String id, String type) {
        int code = (id + "_" + type).hashCode();
        log("Generated requestCode=" + code + " for id=" + id + ", type=" + type);
        return code;
    }

    // ------------------- GLOBAL TIME (PREFS) -------------------

    /**
     * Sets the global notification time used when hour/minute are not provided.
     */
    public static void setGlobalTime(Context context, int hour24, int minute) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_HOUR, hour24)
                .putInt(KEY_MINUTE, minute)
                .apply();
    }

    /**
     * Gets the global notification hour (24h).
     */
    public static int getGlobalHour(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    /**
     * Gets the global notification minute.
     */
    public static int getGlobalMinute(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    // ------------------- SCHEDULE -------------------

    /**
     * Schedules a one-time INEXACT notification for a specific date ("MM/dd/yyyy") at hour/minute.
     * If hour/minute are null, uses the global notification time.
     * <p>
     * Implementation: setWindow(triggerAtMillis, windowLength)
     *
     * @param context     Application context
     * @param receiver    BroadcastReceiver class that will receive the alarm
     * @param title       Notification title
     * @param message     Notification message
     * @param dateString  Date string in "MM/dd/yyyy" format
     * @param requestCode Unique request code for the PendingIntent
     * @param hour        Hour of day (24h) for notification; defaults to 9 if null
     * @param minute      Minute for notification; defaults to 0 if null
     */
    public static void schedule(Context context,
                                Class<?> receiver,
                                String title,
                                String message,
                                String dateString,
                                int requestCode,
                                @Nullable Integer hour,
                                @Nullable Integer minute
    ) {

        log("Attempting to schedule notification: requestCode=" + requestCode +
                ", title=" + title + ", message=" + message + ", date=" + dateString +
                ", hour=" + hour + ",minute=" + minute);

        int notifHour = (hour != null) ? hour : getGlobalHour(context);
        int notifMinute = (minute != null) ? minute : getGlobalMinute(context);

        Date date = parseDate(dateString);
        if (date == null) {
            log("Failed to parse date: " + dateString);
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, notifHour);
        calendar.set(Calendar.MINUTE, notifMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        scheduleAtMillis(context, receiver, title, message, calendar.getTimeInMillis(), requestCode);
    }

    // ------------------- SCHEDULE (MILLIS) -------------------

    /**
     * Schedules a one-time INEXACT notification at triggerAtMillis.
     * The system may deliver it any time within ONE_TIME_WINDOW_MILLIS after triggerAtMillis.
     */
    public static void scheduleAtMillis(Context context,
                                        Class<?> receiver,
                                        String title,
                                        String message,
                                        long triggerAtMillis,
                                        int requestCode
    ) {
        log("Attempting schedule(millis): requestCode=" + requestCode + ", triggerAtMillis="
                + triggerAtMillis);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            log("AlarmManager is null, cannot schedule");
            return;
        }

        PendingIntent pi = buildPendingIntent(context, receiver, title, message, requestCode);

        // Use setWindow so alarms are inexact/batched
        am.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                ONE_TIME_WINDOW_MILLIS,
                pi
        );

        log("Scheduled inexact window alarm: requestCode=" + requestCode);
    }

    // ------------------- SCHEDULE (REPEATING) -------------------

    /**
     * Schedules a repeating INEXACT notification every N days, starting at firstTriggerMillis.
     *
     * Implementation: setInexactRepeating(...)
     */
    public static void scheduleRepeatingDays(Context context,
                                             Class<?> receiver,
                                             String title,
                                             String message,
                                             long firstTriggerMillis,
                                             int intervalDays,
                                             int requestCode
    ) {
        log("Attempting scheduleRepeatingDays: requestCode=" + requestCode +
                ", intervalDays=" + intervalDays + ", firstTriggerMillis=" + firstTriggerMillis);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            log("AlarmManager is null, cannot schedule repeating");
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

        log("Scheduled inexact repeating alarm: requestCode=" + requestCode);
    }

    /**
     * Convenience: schedule "monthly/quarterly/yearly" style reminders using day intervals.
     * (Monthly = 30 days, Quarterly = 90 days, Yearly = 365 days)
     */
    public static void scheduleKitFrequency(Context context,
                                            Class<?> receiver,
                                            String title,
                                            String message,
                                            String frequency,
                                            int requestCode
    ) {
        int intervalDays;
        switch (frequency) {
            case "MONTHLY":
                intervalDays = 30;
                break;
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

        long first = nextGlobalTriggerMillis(context);
        scheduleRepeatingDays(context, receiver, title, message, first, intervalDays, requestCode);
    }

    // ------------------- HELPERS (DATES / NEXT TIME) -------------------

    @Nullable
    public static String subtractDays(String baseDateString, int daysBefore) {
        Date base = parseDate(baseDateString);
        if (base == null) return null;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(base);
        calendar.add(Calendar.DAY_OF_YEAR, -daysBefore);

        return new SimpleDateFormat(DATE_PATTERN, Locale.US).format(calendar.getTime());
    }

    /**
     * Next trigger at the global notification time.
     * If today's time has passed, returns tomorrow at that time.
     */
    public static long nextGlobalTriggerMillis(Context context) {
        int h = getGlobalHour(context);
        int m = getGlobalMinute(context);

        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();

        next.set(Calendar.HOUR_OF_DAY, h);
        next.set(Calendar.MINUTE, m);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis();
    }

    // ------------------- CANCEL -------------------

    /**
     * Cancels a previously scheduled notification.
     *
     * @param context     Application context
     * @param receiver    BroadcastReceiver class
     * @param requestCode Request code used when scheduling
     */
    public static void cancel(Context context, Class<?> receiver, int requestCode) {
        log("Attempting to cancel notification: requestCode=" + requestCode);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            log("AlarmManager is null, cannot cancel");
            return;
        }

        Intent intent = new Intent(context, receiver);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        am.cancel(pi);
        log("Cancelling alarm requestCode=" + requestCode);
    }

    // ------------------- PRIVATE HELPERS -------------------

    @Nullable
    private static Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.US);
            return sdf.parse(dateString);
        } catch (Exception e) {
            log("parseDate failed for " + dateString + ": " + e.getMessage());
            return null;
        }
    }

    private static PendingIntent buildPendingIntent(Context context,
                                                    Class<?> receiver,
                                                    String title,
                                                    String message,
                                                    int requestCode
    ) {
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
}
