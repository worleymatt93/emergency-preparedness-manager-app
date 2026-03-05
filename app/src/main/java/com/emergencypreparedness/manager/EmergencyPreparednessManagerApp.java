package com.emergencypreparedness.manager;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;

/**
 * Custom Application class for Emergency Preparedness Manager.
 * <p>
 * Initializes the notification channel on app startup. Compatible with minSdk 26 (Android 8.0,
 * where Notification Channels were introduced) and tested up to API 36.
 */
public class EmergencyPreparednessManagerApp extends Application {

  //region Constants
  private static final String TAG = "EmergencyPreparednessManagerApp";
  private static final String CHANNEL_ID = "epm_alerts";
  //endregion

  //region Lifecycle
  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "Application onCreate called");
    createNotificationChannel();
  }
  //endregion

  //region Notification Channel Setup

  /**
   * Creates the high-importance notification channel for kit/item reminders. Called once on app
   * startup.
   */
  private void createNotificationChannel() {
    NotificationChannel channel = new NotificationChannel(
        CHANNEL_ID,
        "Emergency Preparedness Alerts",
        NotificationManager.IMPORTANCE_HIGH
    );
    channel.setDescription("Reminders for kits and item expirations");

    NotificationManager manager = getSystemService(NotificationManager.class);
    if (manager == null) {
      Log.e(TAG, "NotificationManager unavailable - channel not created");
      return;
    }

    manager.createNotificationChannel(channel);
    Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
  }
  //endregion
}
