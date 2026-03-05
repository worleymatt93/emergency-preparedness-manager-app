package com.emergencypreparedness.manager.ui.receivers;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.emergencypreparedness.manager.database.Repository;

public class BootAndUpdateReceiver extends BroadcastReceiver {
  private static final String TAG = "BootAndUpdateReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = (intent == null) ? null : intent.getAction();
    Log.d(TAG, "Received broadcast: " + action);

    if (Intent.ACTION_BOOT_COMPLETED.equals(action)
        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
        || Intent.ACTION_TIME_CHANGED.equals(action)
        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {

      Log.d(TAG, "Rescheduling all notifications...");

      Repository repository = new Repository((Application) context.getApplicationContext());

      repository.rescheduleAllNotifications(context);
    }
  }
}
