package com.example.emergencypreparednessmanager.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.adapters.KitItemAdapter;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;
import java.util.Objects;

public class KitDetailsActivity extends AppCompatActivity {

    private static final String KEY_KIT_ID = "kitID";

    // UI

    private MaterialToolbar toolbar;
    private TextInputEditText editKitName, editLocation, editNotes, notificationTimeText;
    private MaterialCheckBox kitNotifyCheckbox;
    private View notificationTimeLayout, emptyItemsText;
    private RecyclerView itemsRecyclerView;
    private FloatingActionButton addItemFab;
    private KitItemAdapter itemAdapter;

    // NotificationTime
    private Integer selectedHour = null;
    private Integer selectedMinute = null;

    // Data
    private Repository repository;
    private Kit currentKit;
    private int kitID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kit_details);

        repository = new Repository(getApplication());

        kitID = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_KIT_ID, -1)
                : getIntent().getIntExtra(KEY_KIT_ID, -1);

        bindViews();
        setupInsets();
        setupToolbar();
        setupNotificationTime();
        setupRecyclerView();
        setupFab();

        if (kitID != -1) {
            loadKit();
            loadItems();
        } else {
            updateItemsVisibility(0);
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.kitNameText);

        editKitName = findViewById(R.id.kitNameText);
        editLocation = findViewById(R.id.locationText);
        editNotes = findViewById(R.id.notesText);

        kitNotifyCheckbox = findViewById(R.id.kitNotifyCheckbox);
        notificationTimeLayout = findViewById(R.id.kitNotificationTimeLayout);
        notificationTimeText = findViewById(R.id.kitNotificationTimeText);

        itemsRecyclerView = findViewById(R.id.kitItemsRecyclerView);
        emptyItemsText = findViewById(R.id.emptyItemsText);
        addItemFab = findViewById(R.id.addKitItemFab);

        notificationTimeLayout.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Applies padding for system bars (status/navigation) to ensure content is not overlapped.
     */
    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupNotificationTime() {
        kitNotifyCheckbox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            notificationTimeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked && selectedHour == null && selectedMinute == null) {
                selectedHour = 9;
                selectedMinute = 0;
                notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
            }
        }));

        notificationTimeText.setOnClickListener(v -> showTimePicker());
    }

    private void showTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour != null ? selectedHour : 9)
                .setMinute(selectedMinute != null ? selectedMinute : 0)
                .setTitleText(getString(R.string.select_notification_time))
                .build();

        picker.addOnPositiveButtonClickListener(view -> {
            selectedHour = picker.getHour();
            selectedMinute = picker.getMinute();
            notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    private void setupRecyclerView() {
        itemAdapter = new KitItemAdapter(this);
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemsRecyclerView.setAdapter(itemAdapter);
        itemsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        itemsRecyclerView.setNestedScrollingEnabled(false);
        itemsRecyclerView.setHasFixedSize(true);

        attachSwipeToDelete();
    }

    private void attachSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target
            ) {
                return false;
            }

            @Override
            public void onSwiped(
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    int direction
            ) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                KitItem deleted = itemAdapter.getItems().get(position);

                itemAdapter.getItems().remove(position);
                itemAdapter.notifyItemRemoved(position);
                updateItemsVisibility(itemAdapter.getItemCount());

                Snackbar.make(itemsRecyclerView, getString(R.string.item_removed), Snackbar.LENGTH_LONG).setAction(getString(R.string.undo), v -> {
                    itemAdapter.getItems().add(position, deleted);
                    itemAdapter.notifyItemInserted(position);
                    updateItemsVisibility(itemAdapter.getItemCount());
                }).addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (event != DISMISS_EVENT_ACTION) {
                            repository.delete(deleted, () -> showToast(getString(R.string.item_deleted)));
                        }
                    }
                }).show();
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(itemsRecyclerView);
    }

    private void setupFab() {
        if (kitID == -1) {
            addItemFab.setAlpha(0.5f);
        }

        addItemFab.setOnClickListener(v -> {
            if (kitID == -1) {
                showToast(getString(R.string.save_kit_before_adding_items));
                return;
            }

            // TODO: Navigate to KitItemDetailsActivity for adding a new item
            // Intent intent = new Intent(this, KitItemDetailsActivity.class);
            // intent.putExtra("kitID", kitID);
            // startActivity(intent);

            showToast(getString(R.string.todo_add_item_screen));
        });
    }

    private void loadKit() {
        repository.getKitById(kitID, kit -> {
            if (kit == null) return;
            currentKit = kit;

            editKitName.setText(kit.getKitName());
            editLocation.setText(kit.getLocation());
            editNotes.setText(kit.getNotes());

            kitNotifyCheckbox.setChecked(kit.isNotificationsEnabled());

            if (kit.isNotificationsEnabled()) {
                notificationTimeLayout.setVisibility(View.VISIBLE);
                selectedHour = kit.getNotifyHour();
                selectedMinute = kit.getNotifyMinute();
                notificationTimeText.setText(formatTime(selectedHour, selectedMinute));
            }
        });
    }

    private void loadItems() {
        repository.getItemsForKit(kitID, items -> {
            itemAdapter.setItems(items);
            updateItemsVisibility(items == null ? 0 : items.size());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (kitID != -1) loadItems();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_KIT_ID, kitID);
        super.onSaveInstanceState(outState);
    }

    private void updateItemsVisibility(int count) {
        emptyItemsText.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_kit_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.kitSave) {
            saveKit(item);
            return true;
        } else if (id == R.id.kitDelete) {
            deleteKit(item);
            return true;
        } else if (id == R.id.share) {
            shareKitToClipboard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveKit(MenuItem item) {
        item.setEnabled(false);
        kitNotifyCheckbox.setEnabled(false);

        String name = Objects.requireNonNull(editKitName.getText()).toString().trim();
        String location = Objects.requireNonNull(editLocation.getText()).toString().trim();
        String notes = Objects.requireNonNull(editNotes.getText()).toString().trim();
        boolean notificationsEnabled = kitNotifyCheckbox.isChecked();

        if (name.isEmpty()) {
            showToast(getString(R.string.kit_name_required));
            item.setEnabled(true);
            kitNotifyCheckbox.setEnabled(true);
            return;
        }

        int hour = selectedHour != null ? selectedHour : 9;
        int minute = selectedMinute != null ? selectedMinute : 0;

        if (kitID == -1) {
            Kit newKit = new Kit(name, location, notes, notificationsEnabled, hour, minute);
            repository.insert(newKit, newId -> {
                kitID = newId.intValue();
                newKit.setKitID(kitID);
                currentKit = newKit;

                showToast(getString(R.string.kit_added));
                finish();
            });
        } else {
            Kit updated = new Kit(kitID, name, location, notes, notificationsEnabled, hour, minute);
            repository.update(updated, () -> {
                currentKit = updated;
                showToast(getString(R.string.kit_updated));
                finish();
            });
        }
    }

    private void deleteKit(MenuItem item) {
        item.setEnabled(false);

        Kit toDelete = new Kit(
                kitID,
                Objects.requireNonNull(editKitName.getText()).toString().trim(),
                Objects.requireNonNull(editLocation.getText()).toString().trim(),
                Objects.requireNonNull(editNotes.getText()).toString().trim(),
                kitNotifyCheckbox.isChecked(),
                selectedHour != null ? selectedHour : 9,
                selectedMinute != null ? selectedMinute : 0
        );

        repository.deleteKitIfNoItems(toDelete, success -> {
            if (!success) {
                showToast(getString(R.string.cannot_delete_kit_with_items));
                item.setEnabled(true);
                return;
            }
            showToast(getString(R.string.kit_deleted));
            finish();
        });
    }

    private void shareKitToClipboard() {
        if (currentKit == null) {
            showToast(getString(R.string.nothing_to_share));
            return;
        }

        repository.getItemsForKit(kitID, items -> {
            StringBuilder sb = new StringBuilder();

            sb.append("Kit: ").append(currentKit.getKitName()).append("\n")
                    .append("Location: ").append(currentKit.getLocation() == null ? "" : currentKit.getLocation()).append("\n")
                    .append("Notes: ").append(currentKit.getNotes() == null ? "" : currentKit.getNotes()).append("\n");

            if (items == null || items.isEmpty()) {
                sb.append("No items.\n");
            } else {
                sb.append("Items:\n");
                for (KitItem ki : items) {
                    sb.append("• ")
                            .append(ki.getItemName())
                            .append(" (Qty: ")
                            .append(ki.getQuantity()).append(")");

                    if (ki.getExpirationDate() != null && !ki.getExpirationDate().trim().isEmpty()) {
                        sb.append(" [Exp: ").append(ki.getExpirationDate().trim()).append("]");
                    }
                    sb.append("\n");
                }
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Kit Details", sb.toString());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                showToast(getString(R.string.copied_to_clipboard));
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
