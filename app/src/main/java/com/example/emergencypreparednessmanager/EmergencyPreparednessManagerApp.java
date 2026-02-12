package com.example.emergencypreparednessmanager;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class EmergencyPreparednessManagerApp extends Application {

    private static final String TAG = "EmergencyPreparednessManagerApp";
    private static final String CHANNEL_ID = "epm_alerts";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Application onCreate() called");
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Emergency Preparedness Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminders for kits and item expirations");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
        } else {
            Log.e(TAG, "NotificationManager was NULL, channel NOT created");
        }
    }
}
