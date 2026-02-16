package com.example.emergencypreparednessmanager.UI.receivers;

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
import com.example.emergencypreparednessmanager.UI.activities.MainActivity;

public class AlertReceiver extends BroadcastReceiver {

    // ------------------- CONSTANTS -------------------

    private static final String TAG = "AlertReceiver";
    private static final String CHANNEL_ID = "epm_alerts";

    // ------------------- LIFECYCLE -------------------

    /**
     * Called when a broadcast is received.
     * Displays a notification with the given title and message,
     * if POST_NOTIFICATIONS permission is granted.
     *
     * @param context   Context in which the receiver is running
     * @param intent    Intent containing notification data:
     *                  "notificationTitle" - the notification title
     *                  "notificationMessage" - the notification message
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received: " + intent);

        if (intent == null) {
            Log.w(TAG, "Intent is null, aborting notification");
            return;
        }

        String title = intent.getStringExtra("notificationTitle");
        String message = intent.getStringExtra("notificationMessage");
        int requestCode = intent.getIntExtra("requestCode", -1);

        Log.d(TAG, "Notification data extracted:");
        Log.d(TAG, "  Title: " + title);
        Log.d(TAG, "  Message: " + message);
        Log.d(TAG, "  RequestCode: " + requestCode);

        // Safety check: if either title or message is null, log and abort
        if (title == null || message == null) {
            Log.w(TAG, "Notification title or message is null, aborting");
            return;
        }

        // Tap action: open the app
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                requestCode != -1 ? requestCode : 0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
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
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, " POST_NOTIFICATIONS permission not granted, cannot show notification");
            return;
        }

        int notifId = (requestCode != -1) ? requestCode : (int) System.currentTimeMillis();
        NotificationManagerCompat.from(context).notify(notifId, builder.build());

        Log.d(TAG, "Notification displayed: id=" + notifId);
    }
}
