package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class KitListActivity extends AppCompatActivity {

    // ------------------- DATA -------------------

    private RecyclerView recyclerView;
    private Repository repository;
    private KitAdapter kitAdapter;

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge layout for modern android devices
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kit_list);

        setupInsets();
        setupRecyclerView();

        repository = new Repository(getApplication());

        setupFab();
        loadKits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadKits(); // Refresh list when return to this screen
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
            public void onSwiped(
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    int direction
            ) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Cache the kit for possible UNDO
                Kit kit = kitAdapter.getKits().get(position);

                // Remove item from adapter for immediate swipe animation
                kitAdapter.getKits().remove(position);
                kitAdapter.notifyItemRemoved(position);

                // Check if kit has items
                repository.deleteKitIfNoItems(kit, success -> {

                    if (!success) {
                        // Kit has items: restore immediately
                        kitAdapter.getKits().add(position, kit);
                        kitAdapter.notifyItemInserted(position);
                        recyclerView.scrollToPosition(position);
                        showToast("Cannot delete a kit that has items");
                        return;
                    }

                    // Kit has no items; show UNDO snackbar
                    Snackbar.make(recyclerView, "Kit removed", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", v -> {

                                // Restore item in adapter
                                kitAdapter.getKits().add(position, kit);
                                kitAdapter.notifyItemInserted(position);

                                // Re-insert into database
                                repository.insert(kit, id -> showToast("Kit restored"));

                            }).addCallback(new Snackbar.Callback() {
                                @Override
                                public void onDismissed(Snackbar transientBottomBar, int event) {
                                    if (event != DISMISS_EVENT_ACTION) {
                                        // Snackbar dismissed without UNDO; commit deletion to DB
                                        repository.delete(kit, () -> showToast("Kit deleted"));
                                    }
                                }
                            }).show();
                });
            }
        };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    /**
     * Configures the FAB to launch KitDetailsActivity for adding a new kit.
     */
    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(KitListActivity.this, KitDetailsActivity.class);
            startActivity(intent);
        });
    }

    // ------------------- DATA LOADING -------------------

    /**
     * Loads all kits asynchronously and updates the RecyclerView adapter.
     * Shows a Toast if no kits are found.
     */
    private void loadKits() {
        repository.getAllKits(kits -> {
            if (kits != null && !kits.isEmpty()) {
                kitAdapter.setKits(kits);
            } else {
                kitAdapter.setKits(kits); // clears list when empty
                showToast("No kits found");
            }
        });
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
