package com.example.emergencypreparednessmanager.UI.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class KitItemEditActivity extends AppCompatActivity {

    // ------------------- CONSTANTS -------------------

    public static final String EXTRA_KIT_ID = "kitID";
    public static final String EXTRA_ITEM_ID = "itemID";

    // ------------------- UI -------------------

    private MaterialToolbar toolbar;

    private TextInputLayout itemNameLayout;
    private TextInputLayout quantityLayout;
    private TextInputLayout categoryLayout;
    private TextInputLayout expirationLayout;
    private TextInputLayout daysBeforeLayout;
    private TextInputLayout notesLayout;

    private TextInputEditText itemNameText, quantityText, expirationText, notesText, editNotifyDaysBefore;
    private MaterialAutoCompleteTextView categoryDropdown;
    private MaterialSwitch switchExpirationReminders, switchNotifyOnZero;
    private MaterialButton btnSaveItem;

    // ------------------- DATA -------------------

    private Repository repository;

    private int kitID = -1;
    private int itemID = -1;

    private List<Category> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private int selectedCategoryId = -1;

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    // ------------------- SETUP -------------------

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
        categoryDropdown = findViewById(R.id.categoryDropdown);
        expirationText = findViewById(R.id.expirationText);
        notesText = findViewById(R.id.notesText);

        switchExpirationReminders = findViewById(R.id.switchExpirationReminders);
        editNotifyDaysBefore = findViewById(R.id.editNotifyDaysBefore);
        switchNotifyOnZero = findViewById(R.id.switchNotifyOnZero);

        btnSaveItem = findViewById(R.id.btnSaveItem);

        // Default: hidden unless switch on
        daysBeforeLayout.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupButtons() {
        btnSaveItem.setOnClickListener(v -> saveItem());
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
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        };
    }

    private void setupCategoryDropdown() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        categoryDropdown.setAdapter(categoryAdapter);

        // Force dropdown to open on tap
        categoryDropdown.setOnClickListener(v -> categoryDropdown.showDropDown());

        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            categoryLayout.setError(null);

            // Last row is "Add category"
            if (position == categories.size()) {
                showAddCategoryDialog();
                return;
            }

            if (position >= 0 && position < categories.size()) {

                Category selected = categories.get(position);

                selectedCategoryId = categories.get(position).getCategoryID();
                categoryDropdown.setText(categories.get(position).getCategoryName(), false);

                // ---- WATER HELPER LOGIC ----
                String name = selected.getCategoryName();

                if (name != null && name.trim().equalsIgnoreCase("Water")) {
                    quantityLayout.setHelperText(getString(R.string.water_quantity_helper));
                } else {
                    quantityLayout.setHelperText(null);
                }

            }
        });
    }

    private void setupExpirationPicker() {
        expirationText.setOnClickListener(v -> showExpirationDatePicker());
    }

    private void setupReminderUi() {
        // Keep days-before visible only when expiration reminders enabled
        switchExpirationReminders.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            daysBeforeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Clear any prior errors when toggling
            daysBeforeLayout.setError(null);
            expirationLayout.setError(null);

            if (!isChecked) {
                // Clean the input when disabling so we don't save stale values
                editNotifyDaysBefore.setText("");
            }
        }));
    }

    // ------------------- LOAD -------------------

    private void loadCategories(@NonNull Runnable afterLoad) {
        repository.getAllCategories(list -> {
            categories = (list != null) ? list : new ArrayList<>();

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
            if (item == null) return;

            itemNameText.setText(item.getItemName());
            quantityText.setText(String.valueOf(item.getQuantity()));
            expirationText.setText(item.getExpirationDate());
            notesText.setText(item.getNotes());

            selectedCategoryId = item.getCategoryID();
            syncDropdownToCategoryId(selectedCategoryId);

            // Restore reminder UI from entity
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
                if (name != null && name.trim().equalsIgnoreCase("Water")) {
                    quantityLayout.setHelperText(getString(R.string.water_quantity_helper));
                } else {
                    quantityLayout.setHelperText(null);
                }

                return;
            }
        }
    }

    // ------------------- CATEGORY DIALOG -------------------

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
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_category))
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(getString(R.string.add), (d, w) -> {
                    String name = (input.getText() == null) ? "" : input.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        layout.setError(getString(R.string.category_name_required));
                        return;
                    }

                    Category category = new Category(name);

                    repository.insert(category, newId -> {
                        // If IGNORE hit because name already exists
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

    // ------------------- DATE PICKER -------------------

    private void showExpirationDatePicker() {
        MaterialDatePicker<Long> picker =
                MaterialDatePicker.Builder.datePicker()
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

    // ------------------- SAVE -------------------

    private void saveItem() {
        btnSaveItem.setEnabled(false);

        // Clear inline errors
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
            if (qty <= 0) throw new NumberFormatException();
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

        // Read reminder UI.
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

        if (itemID == -1) {
            KitItem newItem = new KitItem(
                    kitID,
                    name,
                    qty,
                    selectedCategoryId,
                    exp,
                    notes,
                    expEnabled,
                    daysBefore,
                    zeroEnabled
            );

            repository.insert(newItem, id -> {
                // Need the generated ID for a stable requestCode
                newItem.setItemID(id.intValue());
                applyItemNotificationSchedule(newItem);
                showToast(getString(R.string.item_added));
                finish();
            });
        } else {
            KitItem updated = new KitItem(
                    itemID,
                    kitID,
                    name,
                    qty,
                    selectedCategoryId,
                    exp,
                    notes,
                    expEnabled,
                    daysBefore,
                    zeroEnabled
            );

            repository.update(updated, () -> {
                applyItemNotificationSchedule(updated);
                showToast(getString(R.string.item_updated));
                finish();
            });
        }
    }

    private int readDaysBefore() {
        String s = textOf(editNotifyDaysBefore);
        if (TextUtils.isEmpty(s)) return 0;
        try {
            int v = Integer.parseInt(s);
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(getString(R.string.days_before_invalid));
        }
    }

    // ------------------- NOTIFICATIONS -------------------

    private void applyItemNotificationSchedule(KitItem item) {
        // Expiration reminder
        int expCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()), "ITEM_EXP");
        NotificationScheduler.cancel(this, AlertReceiver.class, expCode);

        if (item.isExpirationRemindersEnabled()) {
            String expDate = item.getExpirationDate();
            String fireDate = NotificationScheduler.subtractDays(expDate, Math.max(0, item.getNotifyDaysBefore()));

            if (!TextUtils.isEmpty(fireDate)) {
                String title = getString(R.string.item_expiration_title, item.getItemName());
                String message = getString(R.string.item_expiration_message, item.getItemName(), expDate);

                // hour/minute null = use global time from Settings
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

        // Notify-on-zero
        int zeroCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()), "ITEM_ZERO");
        NotificationScheduler.cancel(this, AlertReceiver.class, zeroCode);

        if (item.isNotifyOnZero() && item.getQuantity() <= 0) {
            String title = getString(R.string.item_zero_title, item.getItemName());
            String message = getString(R.string.item_zero_message, item.getItemName());

            long trigger = System.currentTimeMillis() + 10_000L;
            NotificationScheduler.scheduleAtMillis(this, AlertReceiver.class, title, message, trigger, zeroCode);
        }
    }

    // ------------------- UTIL -------------------

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
