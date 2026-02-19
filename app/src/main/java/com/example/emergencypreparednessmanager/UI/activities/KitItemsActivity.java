package com.example.emergencypreparednessmanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.emergencypreparednessmanager.UI.adapters.KitItemAdapter;
import com.example.emergencypreparednessmanager.UI.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

public class KitItemsActivity extends AppCompatActivity {

    // ------------------- CONSTANTS -------------------

    public static final String EXTRA_KIT_ID = "kitID";
    public static final String EXTRA_KIT_NAME = "kitName";
    public static final String EXTRA_HIGHLIGHT_ITEM_ID = "highlightItemID";

    private static final long HIGHLIGHT_DURATION_MS = 1200;
    public static final String TAG = "KitItemsActivity";

    // ------------------- UI -------------------

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;

    private View emptyStateLayout;
    private MaterialTextView emptyTitle, emptySubtitle;

    private FloatingActionButton fab;
    private MaterialButton btnAddFirstItem;

    // ------------------- DATA -------------------

    private Repository repository;
    private KitItemAdapter adapter;

    private int kitID = -1;
    private int highlightItemId = -1;
    private String kitName = null;

    private List<KitItem> fullItemList;
    private boolean searchExpanded = false;

    // ------------------- LIFECYCLE -------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_kit_items);

        repository = new Repository(getApplication());

        kitID = getIntent().getIntExtra(EXTRA_KIT_ID, -1);
        kitName = getIntent().getStringExtra(EXTRA_KIT_NAME);
        highlightItemId = getIntent().getIntExtra(EXTRA_HIGHLIGHT_ITEM_ID, -1);

        if (kitID == -1) {
            showToast("Missing kit ID");
            finish();
            return;
        }

        bindViews();
        setupInsets();
        setupToolbar();
        setupRecyclerView();
        setupButtons();

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    // ------------------- BIND / SETUP -------------------

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyTitle = findViewById(R.id.emptyTitle);
        emptySubtitle = findViewById(R.id.emptySubtitle);

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

    private void setupButtons() {
        fab.setOnClickListener(v -> launchAddItem());
        btnAddFirstItem.setOnClickListener(v -> launchAddItem());
    }

    private void launchAddItem() {
        Intent intent = new Intent(this, KitItemEditActivity.class);
        intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, kitID);
        startActivity(intent);
    }

    // ------------------- DATA LOADING -------------------

    private void loadItems() {
        repository.getItemsForKit(kitID, items -> {

            fullItemList = items; // cache
            adapter.setItems(items);

            boolean hasItems = items != null && !items.isEmpty();

            // Visibility for list vs empty state
            applyListVisibility(hasItems);

            // Empty state wording depends on whether search is expanded
            applyEmptyStateMode(searchExpanded);

            // Buttons: when searching, hide add buttons
            if (searchExpanded) {
                btnAddFirstItem.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);
            } else {
                btnAddFirstItem.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                fab.setVisibility(hasItems ? View.VISIBLE: View.GONE);
            }

            // Only do highlight when not searching and list is not empty
            if (!searchExpanded && hasItems) scrollToAndHighlightIfNeeded();
        });
    }

    private void applyListVisibility(boolean showList) {
        recyclerView.setVisibility(showList ? View.VISIBLE : View.GONE);
        emptyStateLayout.setVisibility(showList ? View.GONE : View.VISIBLE);
    }

    private void applyEmptyStateMode(boolean searching) {
        if (searching) {
            emptyTitle.setText(R.string.no_matching_items);
            emptySubtitle.setText(R.string.try_different_search);
        } else {
            emptyTitle.setText(R.string.no_items_in_kit);
            emptySubtitle.setText(R.string.add_your_first_item);
        }
    }

    private int findPositionForItemId(List<KitItem> items, int itemId) {
        if (items == null) return RecyclerView.NO_POSITION;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemID() == itemId) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private void scrollToAndHighlightIfNeeded() {
        if (highlightItemId <= 0) return;
        if (fullItemList == null || fullItemList.isEmpty()) return;

        int position = findPositionForItemId(fullItemList, highlightItemId);
        if (position == RecyclerView.NO_POSITION) {
            highlightItemId = -1;
            return;
        }

        // Apply highlight in adapter first
        adapter.setHighlightedItemId(highlightItemId);

        recyclerView.post(() -> {
            recyclerView.scrollToPosition(position);

            // Clear highlight after a short delay
            recyclerView.postDelayed(() -> adapter.setHighlightedItemId(-1), HIGHLIGHT_DURATION_MS);
        });

        // one-time behavior
        highlightItemId = -1;
    }



    // ------------------- SWIPE TO DELETE -------------------

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

    // ------------------- SEARCH MENU -------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_kit_items, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        if (searchItem == null) {
            Log.e(TAG, "action_search not found in menu");
            return true;
        }

        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView == null) {
            Log.e(TAG, "searchView is null. actionViewClass is not set correctly in the menu XML.");
            return true;
        }

        searchView.setQueryHint(getString(R.string.search_items));

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                searchExpanded = true;

                applyEmptyStateMode(true);
                btnAddFirstItem.setVisibility(View.GONE);
                fab.setVisibility(View.GONE);

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                searchExpanded = false;

                // Restore full list and normal empty state
                adapter.setItems(fullItemList);

                boolean hasItems = fullItemList != null && !fullItemList.isEmpty();
                applyListVisibility(hasItems);

                applyEmptyStateMode(false);
                btnAddFirstItem.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                fab.setVisibility(hasItems ? View.VISIBLE : View.GONE);

                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String q = (newText == null) ? "" : newText.trim();

                if (q.isEmpty()) {
                    // While search is expanded, empty query shows full list
                    adapter.setItems(fullItemList);

                    boolean hasItems = fullItemList != null && !fullItemList.isEmpty();
                    applyListVisibility(hasItems);

                    // Keep search-mode empty text if empty
                    applyEmptyStateMode(true);
                } else {
                    repository.searchItemsInKit(kitID, q, results -> {
                        adapter.setItems(results);

                        boolean hasResults = results != null && !results.isEmpty();
                        applyListVisibility(hasResults);

                        if (!hasResults) applyEmptyStateMode(true);
                    });
                }

                return true;
            }
        });

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
