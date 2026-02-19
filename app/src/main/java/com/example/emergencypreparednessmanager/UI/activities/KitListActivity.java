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
import androidx.appcompat.widget.SearchView;
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
import com.google.android.material.textview.MaterialTextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KitListActivity extends AppCompatActivity {

    // ------------------- UI -------------------

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private MaterialTextView emptyTitle, emptySubtitle;
    private MaterialButton btnCreateFirstKit;
    private FloatingActionButton fab;

    // ------------------- DATA -------------------

    private Repository repository;
    private KitAdapter kitAdapter;
    private List<Kit> fullKitList;
    private boolean searchExpanded = false;

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

        repository = new Repository(getApplication());

        bindViews();
        setupToolbar();
        setupInsets();
        setupRecyclerView();
        setupButtons();

        loadKits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadKits();
    }

    // ------------------- BIND / SETUP -------------------

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);
        btnCreateFirstKit = findViewById(R.id.btnCreateFirstKit);

        fab = findViewById(R.id.floatingActionButton);
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

    /**
     * Sets up RecyclerView and adapter for displaying kits.
     */
    private void setupRecyclerView() {
        kitAdapter = new KitAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(kitAdapter);
        attachSwipeToDelete();
    }

    private void setupButtons() {
        btnCreateFirstKit.setOnClickListener(v -> launchAddKit());
        fab.setOnClickListener(v -> launchAddKit());
    }

    private void launchAddKit() {
        Intent intent = new Intent(this, KitEditActivity.class);
        startActivity(intent);
    }

    // ------------------- DATA LOADING -------------------

    private void loadKits() {
        repository.getAllKits(kits -> {
            fullKitList = kits; // cache
            kitAdapter.setKits(kits);

            boolean hasKits = kits != null && !kits.isEmpty();
            applyListVisibility(hasKits);

            // If search is not open, show the normal empty state and normal buttons
            if (!searchExpanded) {
                applyEmptyStateMode(false);
                fab.setVisibility(hasKits ? View.VISIBLE : View.GONE);
            } else {
                // If search IS open and query is empty, we still want "search mode"
                applyEmptyStateMode(true);
                fab.setVisibility(View.GONE);
            }

            precomputeNonDeletableKits(kits);
        });
    }

    private void applyListVisibility(boolean showList) {
        recyclerView.setVisibility(showList ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(showList ? View.GONE : View.VISIBLE);
    }

    /**
     * Controls what the empty state says and which buttons show.
     * searching=false: normal empty state ("No kits yet" + create button)
     * searching=true: search empty state ("No matching kits" + no create button)
     */
    private void applyEmptyStateMode(boolean searching) {
        if (searching) {
            emptyTitle.setText(R.string.no_matching_kits);
            emptySubtitle.setText(R.string.try_different_search);

            btnCreateFirstKit.setVisibility(View.GONE);
            if (fab != null) fab.setVisibility(View.GONE);
        } else {
            emptyTitle.setText(R.string.no_kits_yet);
            emptySubtitle.setText(R.string.create_your_first_kit_to_start_tracking_supplies);

            boolean hasKits = fullKitList != null && !fullKitList.isEmpty();
            btnCreateFirstKit.setVisibility(hasKits ? View.GONE : View.VISIBLE);
        }
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

    // ------------------- SWIPE TO DELETE -------------------

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

    // ------------------- SEARCH MENU -------------------

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_kit_list, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem == null) return true;

        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView == null) return true;

        searchView.setQueryHint(getString(R.string.search_kits));

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                searchExpanded = true;
                applyEmptyStateMode(true);
                // Hide FAB while searching
                if (fab != null) fab.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                searchExpanded = false;

                // Restore full list and normal empty state
                kitAdapter.setKits(fullKitList);
                boolean hasKits = fullKitList != null && !fullKitList.isEmpty();
                applyListVisibility(hasKits);

                applyEmptyStateMode(false);
                fab.setVisibility(hasKits ? View.VISIBLE : View.GONE);

                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String q = (newText == null) ? "" : newText.trim();

                if (q.isEmpty()) {
                    // While search is expanded, empty query shows full list
                    kitAdapter.setKits(fullKitList);
                    boolean hasKits = fullKitList != null & !fullKitList.isEmpty();
                    applyListVisibility(hasKits);
                } else {
                    repository.searchKits(q, results -> {
                        kitAdapter.setKits(results);
                        boolean hasResults = results != null && !results.isEmpty();
                        applyListVisibility(hasResults);
                    });
                }

                return true;
            }
        });

        return true;
    }

    // ------------------- UTIL -------------------

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
