package com.example.emergencypreparednessmanager.ui.receivers;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.ui.activities.MainActivity;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;

/**
 * BroadcastReceiver that handles scheduled notification alarms.
 * <p>
 * Displays high-priority notifications for kit/item reminders. Respects Android 13+ (API 33)
 * POST_NOTIFICATIONS runtime permission. Compatible with minSdk 26 (Android 8.0) and tested up to
 * API 36.
 */
public class AlertReceiver extends BroadcastReceiver {

  //region Constants
  private static final String TAG = "AlertReceiver";
  private static final String CHANNEL_ID = "epm_alerts";
  //endregion

  //region Lifecycle
  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null) {
      Log.w(TAG, "Received null intent - aborting");
      return;
    }

    String title = intent.getStringExtra("notificationTitle");
    String message = intent.getStringExtra("notificationMessage");
    int requestCode = intent.getIntExtra("requestCode", -1);

    if (title == null || message == null) {
      Log.w(TAG, "Missing title or message in intent - aborting");
      return;
    }

    // Respect app-level notifications enabled setting
    if (!NotificationScheduler.areNotificationsEnabled(context)) {
      Log.d(TAG, "Notifications disabled in settings, skipping display");
      return;
    }

    // Open app on tap
    Intent openIntent = new Intent(context, MainActivity.class);
    openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent contentIntent = PendingIntent.getActivity(
        context,
        requestCode != -1 ? requestCode : 0,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        // Required on API 23+ for security
    );

    // Build notification
    NotificationCompat.Builder builder =
        new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_circle_notifications_24)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

    // Android 13+ runtime permission check
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
      Log.d(TAG, " POST_NOTIFICATIONS permission denied - cannot display");
      return;
    }

    // Use requestCode as ID if provided, else timestamp to avoid duplicates
    int notifId = requestCode != -1 ? requestCode : (int) System.currentTimeMillis();
    NotificationManagerCompat.from(context).notify(notifId, builder.build());

    Log.d(TAG, "Notification displayed: id=" + notifId);
  }
  //endregion
}
