package com.example.emergencypreparednessmanager.ui.activities;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      View focused = getCurrentFocus();

      if (focused instanceof EditText) {
        Rect outRect = new Rect();
        focused.getGlobalVisibleRect(outRect);

        // If the tap is outside the focused EditText, hide keyboard and clear focus
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
}
