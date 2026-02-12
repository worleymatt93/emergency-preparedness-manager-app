package com.example.emergencypreparednessmanager.UI.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
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

    public static final String EXTRA_KIT_ID = "kitID";
    public static final String EXTRA_ITEM_ID = "itemID";

    private MaterialToolbar toolbar;

    private TextInputEditText itemNameText, quantityText, expirationText, notesText;
    private MaterialAutoCompleteTextView categoryDropdown;
    private MaterialButton btnSaveItem;

    private Repository repository;

    private int kitID = -1;
    private int itemID = -1;

    private List<Category> categories = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private int selectedCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kit_item_edit);

        repository = new Repository(getApplication());

        kitID = getIntent().getIntExtra(EXTRA_KIT_ID, -1);
        itemID = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);

        if (kitID == -1) {
            showToast("Missing kit ID");
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupCategoryDropdown();
        setupExpirationPicker();
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

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);

        itemNameText = findViewById(R.id.itemNameText);
        quantityText = findViewById(R.id.quantityText);
        categoryDropdown = findViewById(R.id.categoryDropdown);
        expirationText = findViewById(R.id.expirationText);
        notesText = findViewById(R.id.notesText);

        btnSaveItem = findViewById(R.id.btnSaveItem);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupCategoryDropdown() {
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        categoryDropdown.setAdapter(categoryAdapter);

        // Force dropdown to open on tap
        categoryDropdown.setOnClickListener(v -> categoryDropdown.showDropDown());

        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            // Last row is "Add category"
            if (position == categories.size()) {
                showAddCategoryDialog();
                return;
            }

            if (position >= 0 && position < categories.size()) {
                selectedCategoryId = categories.get(position).getCategoryID();
                categoryDropdown.setText(categories.get(position).getCategoryName(), false);
            }
        });
    }

    private void setupButtons() {
        btnSaveItem.setOnClickListener(v -> saveItem());
    }

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

            // New item: leave blank and force user to choose
            if (itemID != -1) {
                selectedCategoryId = -1;
                categoryDropdown.setText("", false);
            }

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
        });
    }

    private void syncDropdownToCategoryId(int categoryId) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getCategoryID() == categoryId) {
                categoryDropdown.setText(categories.get(i).getCategoryName(), false);
                return;
            }
        }
    }

    private void showAddCategoryDialog() {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(getString(R.string.category_name));

        TextInputEditText input = new TextInputEditText(this);
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_category))
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(getString(R.string.add), (d, w) -> {
                    String name = (input.getText() == null) ? "" : input.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        showToast(getString(R.string.category_name_required));
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

    private void setupExpirationPicker() {
        expirationText.setOnClickListener(v -> showExpirationDatePicker());
    }

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
        });

        picker.show(getSupportFragmentManager(), "EXP_DATE");
    }

    private void saveItem() {
        btnSaveItem.setEnabled(false);

        String name = textOf(itemNameText);
        String qtyStr = textOf(quantityText);
        String exp = textOf(expirationText);
        String notes = textOf(notesText);

        if (TextUtils.isEmpty(name)) {
            showToast(getString(R.string.item_name_required));
            btnSaveItem.setEnabled(true);
            return;
        }

        int qty;
        try {
            qty = TextUtils.isEmpty(qtyStr) ? 1 : Integer.parseInt(qtyStr);
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showToast(getString(R.string.quantity_invalid));
            btnSaveItem.setEnabled(true);
            return;
        }

        if (selectedCategoryId == -1) {
            showToast(getString(R.string.category_required));
            btnSaveItem.setEnabled(true);
            return;
        }

        boolean notificationsEnabled = false;
        int notifyHour = 9;
        int notifyMinute = 0;

        if (itemID == -1) {
            KitItem newItem = new KitItem(
                    kitID,
                    name,
                    qty,
                    selectedCategoryId,
                    exp,
                    notes,
                    notificationsEnabled,
                    notifyHour,
                    notifyMinute
            );

            repository.insert(newItem, id -> {
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
                    notificationsEnabled,
                    notifyHour,
                    notifyMinute
            );

            repository.update(updated, () -> {
                showToast(getString(R.string.item_updated));
                finish();
            });
        }
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    // ------------------- TOAST -------------------

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
