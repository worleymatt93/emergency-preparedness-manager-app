package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.adapters.KitItemAdapter;
import com.example.emergencypreparednessmanager.UI.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class KitItemsActivity extends AppCompatActivity {

    public static final String EXTRA_KIT_ID = "kitID";
    public static final String EXTRA_KIT_NAME = "kitName";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private FloatingActionButton fab;
    private MaterialButton btnAddFirstItem;

    private Repository repository;
    private KitItemAdapter adapter;

    private int kitID = -1;
    private String kitName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kit_items);

        repository = new Repository(getApplication());

        kitID = getIntent().getIntExtra(EXTRA_KIT_ID, -1);
        kitName = getIntent().getStringExtra(EXTRA_KIT_NAME);

        if (kitID == -1) {
            showToast("Missing kit ID");
            finish();
            return;
        }

        bindViews();
        setupInsets();
        setupToolbar();
        setupRecyclerView();
        setupFab();

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fab = findViewById(R.id.floatingActionButton);
        btnAddFirstItem = findViewById(R.id.btnAddFirstItem);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (kitName != null && !kitName.trim().isEmpty()) {
            toolbar.setTitle(kitName.trim());
        } else {
            toolbar.setTitle(getString(R.string.items));
        }

        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new KitItemAdapter(this, (itemId, delta) ->
                repository.adjustItemQuantity(itemId, delta, updatedItem -> {
            if (updatedItem != null) {
                adapter.replaceItem(updatedItem);
            } else {
                loadItems();
            }
        }));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        attachSwipeToDelete();
    }

    /**
     * Attaches swipe-to-delete behavior to the kits RecyclerView.
     * Displays an undo SnackBar before permanently deleting a kit.
     */
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
                // Drag-and-drop is not supported
                return false;
            }

            @Override
            public void onSwiped(
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    int direction
            ) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Cache the item for possible UNDO
                KitItem item = adapter.getItems().get(position);

                // Remove item from adapter for immediate swipe animation
                adapter.getItems().remove(position);
                adapter.notifyItemRemoved(position);

                // Show UNDO snackbar
                Snackbar.make(recyclerView, R.string.item_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> {
                            // Restore item in adapter
                            adapter.getItems().add(position, item);
                            adapter.notifyItemInserted(position);
                            recyclerView.scrollToPosition(position);
                            showToast(getString(R.string.item_restored));
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Cancel any scheduled notifications for this item before deleting
                                    cancelItemNotifications(item);

                                    // Snackbar dismissed without UNDO; commit deletion to DB
                                    repository.delete(item, () -> {
                                        showToast(getString(R.string.item_deleted));
                                        loadItems();
                                    });
                                }
                            }
                        })
                        .show();
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void cancelItemNotifications(KitItem item) {
        int expCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()), "ITEM_EXP");
        NotificationScheduler.cancel(this, AlertReceiver.class, expCode);

        int zeroCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()), "ITEM_ZERO");
        NotificationScheduler.cancel(this, AlertReceiver.class, zeroCode);
    }

    private void launchAddItem() {
        Intent intent = new Intent(this, KitItemEditActivity.class);
        intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, kitID);
        startActivity(intent);
    }

    private void setupFab() {
        fab.setOnClickListener(v -> launchAddItem());
        btnAddFirstItem.setOnClickListener(v -> launchAddItem());
    }

    private void loadItems() {
        repository.getItemsForKit(kitID, items -> {
            adapter.setItems(items);

            int count = (items == null) ? 0 : items.size();
            boolean empty = count == 0;

            emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

            btnAddFirstItem.setVisibility(empty ? View.VISIBLE : View.GONE);
            fab.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_kit_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_edit_kit) {
            Intent intent = new Intent(this, KitEditActivity.class);
            intent.putExtra(KitEditActivity.EXTRA_KIT_ID, kitID);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Displays a short Toast message.
     * Use this for simple non-interactive messages.
     */
    private void showToast(String message) {
        if (message.length() >= 30) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
}
