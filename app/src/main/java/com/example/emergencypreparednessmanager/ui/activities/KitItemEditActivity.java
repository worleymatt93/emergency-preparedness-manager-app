package com.example.emergencypreparednessmanager.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.ui.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.util.AppConstants;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

/**
 * Activity for creating or editing a kit item.
 * <p>
 * Handles item name, quantity, category, expiration date, notes, and reminder settings.
 * Schedules/cancels notifications on save.
 */
public class KitItemEditActivity extends BaseActivity {

  //region Constants
  public static final String EXTRA_KIT_ID = "kitID";
  public static final String EXTRA_ITEM_ID = "itemID";
  //endregion

  //region Fields
  private MaterialToolbar toolbar;

  private TextInputLayout itemNameLayout;
  private TextInputLayout quantityLayout;
  private TextInputLayout categoryLayout;
  private TextInputLayout expirationLayout;
  private TextInputLayout daysBeforeLayout;
  private TextInputLayout notesLayout;

  private TextInputEditText itemNameText;
  private TextInputEditText quantityText;
  private TextInputEditText expirationText;
  private TextInputEditText notesText;
  private TextInputEditText editNotifyDaysBefore;

  private MaterialAutoCompleteTextView categoryDropdown;
  private MaterialSwitch switchExpirationReminders;
  private MaterialSwitch switchNotifyOnZero;

  private MaterialButton btnSaveItem;
  private MaterialButton btnDeleteItem;

  private Repository repository;

  private int kitID = -1;
  private int itemID = -1;

  private List<Category> categories = new ArrayList<>();
  private ArrayAdapter<String> categoryAdapter;
  private int selectedCategoryId = -1;
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_kit_item_edit);

    repository = new Repository(getApplication());

    kitID = getIntent().getIntExtra(EXTRA_KIT_ID, -1);
    itemID = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);

    if (kitID == -1) {
      showToast(getString(R.string.missing_kit_id));
      finish();
      return;
    }

    bindViews();
    setupInsets();
    setupToolbar();
    setupCategoryDropdown();
    setupExpirationPicker();
    setupReminderUi();
    setupFieldBehaviors();
    setupButtons();

    loadCategories(() -> {
      if (itemID != -1) {
        loadItem();
        toolbar.setTitle(getString(R.string.edit_item));
      } else {
        toolbar.setTitle(getString(R.string.add_item));
      }
    });
  }
  //endregion

  //region Setup
  private void bindViews() {
    toolbar = findViewById(R.id.toolbar);

    itemNameLayout = findViewById(R.id.itemNameLayout);
    quantityLayout = findViewById(R.id.quantityLayout);
    categoryLayout = findViewById(R.id.categoryLayout);
    expirationLayout = findViewById(R.id.expirationLayout);
    daysBeforeLayout = findViewById(R.id.daysBeforeLayout);
    notesLayout = findViewById(R.id.notesLayout);

    itemNameText = findViewById(R.id.itemNameText);
    quantityText = findViewById(R.id.quantityText);
    expirationText = findViewById(R.id.expirationText);
    notesText = findViewById(R.id.notesText);
    editNotifyDaysBefore = findViewById(R.id.editNotifyDaysBefore);

    categoryDropdown = findViewById(R.id.categoryDropdown);
    switchExpirationReminders = findViewById(R.id.switchExpirationReminders);
    switchNotifyOnZero = findViewById(R.id.switchNotifyOnZero);

    btnSaveItem = findViewById(R.id.btnSaveItem);
    btnDeleteItem = findViewById(R.id.btnDeleteItem);

    // Hide frequency section initially
    daysBeforeLayout.setVisibility(View.GONE);

    // Hide Delete button on create
    btnDeleteItem.setVisibility(itemID != -1 ? View.VISIBLE : View.GONE);
  }

  private void setupInsets() {
    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    if (toolbar == null) {
      return;
    }

    ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
      WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
          Objects.requireNonNull(insets.toWindowInsets()));
      androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
          WindowInsetsCompat.Type.systemBars());

      // Apply insets padding – background under status bar
      toolbar.setPadding(
          systemBars.left,
          systemBars.top,
          systemBars.right,
          toolbar.getPaddingBottom()
      );

      toolbar.setTitleCentered(true);

      return insets;
    });

    // Bottom padding for scroll content
    NestedScrollView scrollView = findViewById(R.id.scrollView);
    if (scrollView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            v.getPaddingLeft(),
            v.getPaddingTop(),
            v.getPaddingRight(),
            systemBars.bottom
        );

        return insets;
      });
    }
  }

  private void setupToolbar() {
    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
  }

  private void setupCategoryDropdown() {
    categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
        new ArrayList<>());
    categoryDropdown.setAdapter(categoryAdapter);

    categoryDropdown.setOnClickListener(v -> categoryDropdown.showDropDown());

    categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
      categoryLayout.setError(null);

      if (position == categories.size()) {
        showAddCategoryDialog();
        return;
      }

      if (position >= 0 && position < categories.size()) {
        Category selected = categories.get(position);
        selectedCategoryId = categories.get(position).getCategoryID();
        categoryDropdown.setText(categories.get(position).getCategoryName(), false);

        String name = selected.getCategoryName();
        quantityLayout.setHelperText(getString(R.string.water).equalsIgnoreCase(name)
            ? getString(R.string.water_quantity_helper)
            : null);
      }
    });
  }

  private void setupExpirationPicker() {
    expirationText.setOnClickListener(v -> showExpirationDatePicker());
  }

  private void setupReminderUi() {
    switchExpirationReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
      daysBeforeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      daysBeforeLayout.setError(null);
      expirationLayout.setError(null);

      if (!isChecked) {
        editNotifyDaysBefore.setText("");
      }
    });
  }

  private void setupFieldBehaviors() {
    itemNameText.addTextChangedListener(clearErrorWatcher(itemNameLayout));
    quantityText.addTextChangedListener(clearErrorWatcher(quantityLayout));
    categoryDropdown.addTextChangedListener(clearErrorWatcher(categoryLayout));
    expirationText.addTextChangedListener(clearErrorWatcher(expirationLayout));
    editNotifyDaysBefore.addTextChangedListener(clearErrorWatcher(daysBeforeLayout));
    notesText.addTextChangedListener(clearErrorWatcher(notesLayout));
  }

  private TextWatcher clearErrorWatcher(TextInputLayout layout) {
    return new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        layout.setError(null);
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    };
  }

  private void setupButtons() {
    btnSaveItem.setOnClickListener(v -> saveItem());
    btnDeleteItem.setOnClickListener(v -> confirmDeleteItem());
  }
  //endregion

  //region Load & Category Dialog
  private void loadCategories(@NonNull Runnable afterLoad) {
    repository.getAllCategories(list -> {
      categories = list != null ? list : new ArrayList<>();

      List<String> names = new ArrayList<>();
      for (Category c : categories) {
        names.add(c.getCategoryName());
      }
      names.add(getString(R.string.add_category_ellipsis));

      categoryAdapter.clear();
      categoryAdapter.addAll(names);
      categoryAdapter.notifyDataSetChanged();

      afterLoad.run();
    });
  }

  private void loadItem() {
    repository.getItemById(itemID, item -> {
      if (item == null) {
        return;
      }

      itemNameText.setText(item.getItemName());
      quantityText.setText(String.valueOf(item.getQuantity()));
      expirationText.setText(item.getExpirationDate());
      notesText.setText(item.getNotes());

      selectedCategoryId = item.getCategoryID();
      syncDropdownToCategoryId(selectedCategoryId);

      switchExpirationReminders.setChecked(item.isExpirationRemindersEnabled());
      if (item.isExpirationRemindersEnabled()) {
        daysBeforeLayout.setVisibility(View.VISIBLE);
        editNotifyDaysBefore.setText(String.valueOf(item.getNotifyDaysBefore()));
      } else {
        daysBeforeLayout.setVisibility(View.GONE);
        editNotifyDaysBefore.setText("");
      }

      switchNotifyOnZero.setChecked(item.isNotifyOnZero());
    });
  }

  private void syncDropdownToCategoryId(int categoryId) {
    for (Category c : categories) {
      if (c.getCategoryID() == categoryId) {
        categoryDropdown.setText(c.getCategoryName(), false);

        String name = c.getCategoryName();
        quantityLayout.setHelperText(getString(R.string.water).equalsIgnoreCase(name)
            ? getString(R.string.water_quantity_helper)
            : null);
        return;
      }
    }
  }
  //endregion

  //region Category Dialog
  private void showAddCategoryDialog() {
    TextInputLayout layout = new TextInputLayout(this);
    layout.setHint(getString(R.string.category_name));

    TextInputEditText input = new TextInputEditText(this);
    layout.addView(input);

    input.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        layout.setError(null);
      }

      @Override
      public void afterTextChanged(Editable s) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }
    });

    new AlertDialog.Builder(this)
        .setTitle(getString(R.string.add_category))
        .setView(layout)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(getString(R.string.add), (d, w) -> {
          String name = input.getText() == null ? "" : input.getText().toString().trim();

          if (TextUtils.isEmpty(name)) {
            layout.setError(getString(R.string.category_name_required));
            return;
          }

          Category category = new Category(name);

          repository.insertCategory(category, newId -> {
            if (newId == -1L) {
              showToast(getString(R.string.category_already_exists));
              return;
            }

            category.setCategoryID(newId.intValue());

            loadCategories(() -> {
              selectedCategoryId = category.getCategoryID();
              categoryDropdown.setText(category.getCategoryName(), false);
              showToast(getString(R.string.category_added));
            });
          });
        }).show();
  }
  //endregion

  //region Date Picker
  private void showExpirationDatePicker() {
    MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
        .setTitleText(getString(R.string.select_expiration_date))
        .build();

    picker.addOnPositiveButtonClickListener(selection -> {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
      sdf.setTimeZone(TimeZone.getDefault());
      String date = sdf.format(new Date(selection));
      expirationText.setText(date);
      expirationLayout.setError(null);
    });

    picker.show(getSupportFragmentManager(), "EXP_DATE");
  }
  //endregion

  //region Save
  private void saveItem() {
    btnSaveItem.setEnabled(false);

    itemNameLayout.setError(null);
    quantityLayout.setError(null);
    categoryLayout.setError(null);
    expirationLayout.setError(null);
    daysBeforeLayout.setError(null);
    notesLayout.setError(null);

    String name = textOf(itemNameText);
    String qtyStr = textOf(quantityText);
    String exp = textOf(expirationText);
    String notes = textOf(notesText);

    if (TextUtils.isEmpty(name)) {
      itemNameLayout.setError(getString(R.string.item_name_required));
      btnSaveItem.setEnabled(true);
      return;
    }

    int qty;
    try {
      qty = TextUtils.isEmpty(qtyStr) ? 1 : Integer.parseInt(qtyStr);
      if (qty <= 0) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      quantityLayout.setError(getString(R.string.quantity_invalid));
      btnSaveItem.setEnabled(true);
      return;
    }

    if (selectedCategoryId == -1) {
      categoryLayout.setError(getString(R.string.category_required));
      btnSaveItem.setEnabled(true);
      return;
    }

    boolean expEnabled = switchExpirationReminders.isChecked();
    int daysBefore = 0;

    if (expEnabled) {
      if (TextUtils.isEmpty(exp)) {
        expirationLayout.setError(getString(R.string.expiration_required_for_reminder));
        btnSaveItem.setEnabled(true);
        return;
      }

      try {
        daysBefore = readDaysBefore();
      } catch (IllegalArgumentException e) {
        daysBeforeLayout.setError(e.getMessage());
        btnSaveItem.setEnabled(true);
        return;
      }
    }

    boolean zeroEnabled = switchNotifyOnZero.isChecked();

    KitItem item = itemID == -1
        ? new KitItem(kitID, name, qty, selectedCategoryId, exp, notes, expEnabled, daysBefore,
        zeroEnabled)
        : new KitItem(itemID, kitID, name, qty, selectedCategoryId, exp, notes, expEnabled,
            daysBefore, zeroEnabled);

    if (itemID == -1) {
      repository.insertItem(item, id -> {
        item.setItemID(id.intValue());
        applyItemNotificationSchedule(item);
        showToast(getString(R.string.item_added));
        finish();
      });
    } else {
      repository.updateItem(item, () -> {
        applyItemNotificationSchedule(item);
        showToast(getString(R.string.item_updated));
        finish();
      });
    }
  }

  private int readDaysBefore() {
    String s = textOf(editNotifyDaysBefore);
    if (TextUtils.isEmpty(s)) {
      return 0;
    }

    try {
      int v = Integer.parseInt(s);
      if (v < 0) {
        throw new NumberFormatException();
      }
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(getString(R.string.days_before_invalid));
    }
  }

  private void applyItemNotificationSchedule(KitItem item) {
    int expCode = NotificationScheduler.generateRequestCode(
        String.valueOf(item.getItemID()), "ITEM_EXP");
    NotificationScheduler.cancel(this, AlertReceiver.class, expCode);

    if (item.isExpirationRemindersEnabled()) {
      String expDate = item.getExpirationDate();
      String fireDate = NotificationScheduler.subtractDays(expDate,
          Math.max(0, item.getNotifyDaysBefore()));

      if (!TextUtils.isEmpty(fireDate)) {
        String title = getString(R.string.item_expiration_title, item.getItemName());
        String message = getString(R.string.item_expiration_message, item.getItemName(), expDate);

        NotificationScheduler.schedule(
            this,
            AlertReceiver.class,
            title,
            message,
            fireDate,
            expCode,
            null,
            null
        );
      }
    }

    int zeroCode = NotificationScheduler.generateRequestCode(String.valueOf(
        item.getItemID()), "ITEM_ZERO");
    NotificationScheduler.cancel(this, AlertReceiver.class, zeroCode);

    if (item.isNotifyOnZero() && item.getQuantity() <= 0) {
      String title = getString(R.string.item_zero_title, item.getItemName());
      String message = getString(R.string.item_zero_message, item.getItemName());

      long trigger = System.currentTimeMillis() + AppConstants.ZERO_QUANTITY_ALERT_DELAY_MS;
      NotificationScheduler.scheduleAtMillis(this, AlertReceiver.class, title, message, trigger,
          zeroCode);
    }
  }
  //endregion

  //region Delete Item
  private void confirmDeleteItem() {
    showDeleteConfirmation(getString(R.string.delete_item), "item", this::performDeleteItem);
  }

  private void performDeleteItem() {
    btnDeleteItem.setEnabled(false);

    KitItem itemToDelete = new KitItem();
    itemToDelete.setItemID(itemID);

    repository.deleteItem(itemToDelete, () -> {
      showToast(getString(R.string.item_deleted));

      Intent intent = new Intent(this, KitItemsActivity.class);
      intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, kitID);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intent);
      finish();
    });
  }
  //endregion

  //region Helpers
  private void showDeleteConfirmation(String title, String type, Runnable onConfirm) {
    new MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(getString(R.string.permanent_delete_warning, type) + "\n" +
            getString(R.string.cannot_be_undone))
        .setPositiveButton(
            getString(R.string.delete), (dialog, which) -> onConfirm.run()
        )
        .setNegativeButton(android.R.string.cancel, null)
        .setIconAttribute(android.R.attr.alertDialogIcon)
        .show();
  }

  private String textOf(TextInputEditText editText) {
    return editText.getText() == null ? "" : editText.getText().toString().trim();
  }

  private void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
        .show();
  }
  //endregion
}
