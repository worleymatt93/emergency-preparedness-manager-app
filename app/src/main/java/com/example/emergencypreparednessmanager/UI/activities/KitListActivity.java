package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.emergencypreparednessmanager.UI.adapters.KitAdapter;
import com.example.emergencypreparednessmanager.UI.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KitListActivity extends AppCompatActivity {

    // ------------------- UI / DATA -------------------

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private FloatingActionButton fab;

    private Repository repository;
    private KitAdapter kitAdapter;

    /**
     * Precomputed set of kits that have at least 1 item.
     * If a kit ID is in here, we block swipe-to-delete immediately with no Snackbar.
     */
    private final Set<Integer> nonDeletableKitIds = new HashSet<>();

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge layout for modern android devices
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kit_list);

        toolbar = findViewById(R.id.toolbar);
        setupToolbar();

        setupInsets();
        setupRecyclerView();

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        MaterialButton btnCreateFirstKit = findViewById(R.id.btnCreateFirstKit);

        btnCreateFirstKit.setOnClickListener(v -> {
            Intent intent = new Intent(KitListActivity.this, KitEditActivity.class);
            startActivity(intent);
        });

        repository = new Repository(getApplication());

        setupFab();
        loadKits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadKits(); // Refresh list when return to this screen
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    // ------------------- SETUP -------------------

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

    /**
     * Sets up RecyclerView and adapter for displaying kits.
     */
    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);

        kitAdapter = new KitAdapter(this);
        recyclerView.setAdapter(kitAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        attachSwipeToDelete();
    }

    /**
     * Attaches swipe-to-delete behavior to the kits RecyclerView.
     * Prevents deletion if the kit contains items.
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
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Cache the kit for possible UNDO
                Kit kit = kitAdapter.getKits().get(position);
                int kitId = kit.getKitID();

                // If the kit has items, block immediately
                if (nonDeletableKitIds.contains(kit.getKitID())) {
                    kitAdapter.notifyItemChanged(position);
                    showToast(getString(R.string.cannot_delete_kit_with_items));
                    return;
                }

                // No items: proceed with the real remove & snackbar flow
                kitAdapter.getKits().remove(position);
                kitAdapter.notifyItemRemoved(position);

                Snackbar.make(recyclerView, R.string.kit_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, v -> {
                            // Restore in adapter only
                            kitAdapter.getKits().add(position, kit);
                            kitAdapter.notifyItemInserted(position);
                            recyclerView.scrollToPosition(position);

                            showToast(getString(R.string.kit_restored));
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar transientBottomBar, int event) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    // Commit deletion to db
                                    repository.delete(kit, () -> {
                                        showToast(getString(R.string.kit_deleted));
                                        loadKits();
                                    });
                                }
                            }
                        })
                        .show();
                }
            };

            new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    private void cancelKitNotifications(Kit kit) {
        int requestCode = NotificationScheduler.generateRequestCode(String.valueOf(kit.getKitID()), "KIT");
        NotificationScheduler.cancel(this, AlertReceiver.class, requestCode);
    }

    /**
     * Configures the FAB to launch KitDetailsActivity for adding a new kit.
     */
    private void setupFab() {
        fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(KitListActivity.this, KitEditActivity.class);
            startActivity(intent);
        });
    }

    // ------------------- DATA LOADING -------------------

    private void loadKits() {
        repository.getAllKits(kits -> {
            boolean hasKits = kits != null && !kits.isEmpty();

            kitAdapter.setKits(kits); // works for both empty and non-empty

            recyclerView.setVisibility(hasKits ? View.VISIBLE : View.GONE);
            emptyStateLayout.setVisibility(hasKits ? View.GONE : View.VISIBLE);

            // Hide FAB when empty
            if (fab != null) {
                fab.setVisibility(hasKits ? View.VISIBLE : View.GONE);
            }

            // Precompute which kitIDs have items
            precomputeNonDeletableKits(kits);
        });
    }

    private void precomputeNonDeletableKits(List<Kit> kits) {
        nonDeletableKitIds.clear();

        if (kits == null || kits.isEmpty()) return;

        for (Kit kit : kits) {
            final int kitId = kit.getKitID();

            repository.getItemsForKit(kitId, items -> {
                if (items != null && !items.isEmpty()) {
                    nonDeletableKitIds.add(kitId);
                } else {
                    nonDeletableKitIds.remove(kitId);
                }
            });
        }
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
