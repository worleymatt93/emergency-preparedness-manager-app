package com.example.emergencypreparednessmanager.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.emergencypreparednessmanager.R;

/**
 * Base activity that provides shared behavior across screens:
 * - Dismiss keyboard when tapping outside a focused EditText
 * - Helpers for Android 13+ notification permission
 */
public abstract class BaseActivity extends AppCompatActivity {

  private static final int REQ_NOTIFICATIONS = 1001;

  /**
   * Requests POST_NOTIFICATIONS permission on Android 13+ if not already granted.
   * Safe to call multiple times.
   */
  protected void ensureNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
    if (hasNotificationPermission()) return;

    ActivityCompat.requestPermissions(
        this,
        new String[]{Manifest.permission.POST_NOTIFICATIONS},
        REQ_NOTIFICATIONS
    );
  }

  /**
   * @return true if the app can post notifications (or if runtime permission is not required).
   */
  protected boolean hasNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode,
      @NonNull String[] permissions,
      @NonNull int[] grantResults
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode != REQ_NOTIFICATIONS) return;

    boolean granted = grantResults.length > 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED;

    if (granted) {
      showToast(getString(R.string.notifications_enabled));
    } else {
      showToast(getString(R.string.notifications_denied));
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      View focused = getCurrentFocus();
      if (focused instanceof EditText) {
        Rect outRect = new Rect();
        focused.getGlobalVisibleRect(outRect);
        if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
          focused.clearFocus();
          hideKeyboard(focused);
        }
      }
    }
    return super.dispatchTouchEvent(ev);
  }

  private void hideKeyboard(View view) {
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  protected void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30
        ? Toast.LENGTH_LONG
        : Toast.LENGTH_SHORT).show();
  }
}