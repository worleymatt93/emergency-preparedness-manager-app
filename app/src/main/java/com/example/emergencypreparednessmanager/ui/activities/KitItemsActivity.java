package com.example.emergencypreparednessmanager.ui.activities;

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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.ui.adapters.KitItemAdapter;
import com.example.emergencypreparednessmanager.ui.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity displaying the list of items in a specific kit.
 * <p>
 * Features:
 * <ul>
 *   <li>RecyclerView of items with quantity controls</li>
 *   <li>Swipe-to-delete with undo Snackbar</li>
 *   <li>FAB and empty-state "Add First Item" button</li>
 *   <li>Search bar with live filtering within the kit</li>
 *   <li>Highlight new/updated item on return from edit</li>
 * </ul>
 */
public class KitItemsActivity extends AppCompatActivity {

  //region Constants
  public static final String EXTRA_KIT_ID = "kitID";
  public static final String EXTRA_KIT_NAME = "kitName";
  public static final String EXTRA_HIGHLIGHT_ITEM_ID = "highlightItemID";
  public static final String TAG = "KitItemsActivity";
  private static final long HIGHLIGHT_DURATION_MS = 1200;
  //endregion

  //region Fields
  private MaterialToolbar toolbar;
  private RecyclerView recyclerView;
  private View emptyStateLayout;
  private MaterialTextView emptyTitle, emptySubtitle;
  private MaterialButton btnAddFirstItem;
  private FloatingActionButton fab;

  private Repository repository;
  private KitItemAdapter adapter;

  private int kitID = -1;
  private String kitName;
  private int highlightItemId = -1;

  private List<KitItem> fullItemList;
  private boolean searchExpanded = false;
  //endregion

  //region Lifecycle
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
      showToast(getString(R.string.missing_kit_id));
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
    loadItems(); // Refresh on return (e.g., after edit/add)
  }
  //endregion

  //region Setup
  private void bindViews() {
    toolbar = findViewById(R.id.toolbar);
    recyclerView = findViewById(R.id.recyclerView);
    emptyStateLayout = findViewById(R.id.emptyStateLayout);
    emptyTitle = findViewById(R.id.emptyTitle);
    emptySubtitle = findViewById(R.id.emptySubtitle);
    btnAddFirstItem = findViewById(R.id.btnAddFirstItem);
    fab = findViewById(R.id.floatingActionButton);
  }

  private void setupInsets() {
    // Toolbar: top clearance for status bar
    if (toolbar != null) {
      ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
        WindowInsetsCompat insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
            Objects.requireNonNull(insets.toWindowInsets()));
        androidx.core.graphics.Insets systemBars = insetsCompat.getInsets(
            WindowInsetsCompat.Type.systemBars());

        v.setPadding(
            systemBars.left,
            systemBars.top,
            systemBars.right,
            v.getPaddingBottom()
        );

        toolbar.setTitleCentered(true);

        return insets;
      });
    }

    // RecyclerView: bottom clearance for nav/gesture bar
    if (recyclerView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
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
  //endregion

  //region Data Loading
  private void loadItems() {
    repository.getItemsForKit(kitID, items -> {
      fullItemList = items;

      adapter.setItems(items);

      boolean hasItems = items != null && !items.isEmpty();
      applyListVisibility(hasItems);
      applyEmptyStateMode(searchExpanded);

      if (searchExpanded) {
        btnAddFirstItem.setVisibility(View.GONE);
        fab.setVisibility(View.GONE);
      } else {
        btnAddFirstItem.setVisibility(hasItems ? View.GONE : View.VISIBLE);
        fab.setVisibility(hasItems ? View.VISIBLE : View.GONE);
      }

      if (!searchExpanded && hasItems) {
        scrollToAndHighlightIfNeeded();
      }
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

  private void scrollToAndHighlightIfNeeded() {
    if (highlightItemId <= 0) {
      return;
    }

    int position = adapter.findPositionByItemId(highlightItemId);
    if (position == RecyclerView.NO_POSITION) {
      highlightItemId = -1;
      return;
    }

    adapter.setHighlightedItemId(highlightItemId);

    recyclerView.post(() -> {
      recyclerView.scrollToPosition(position);
      recyclerView.postDelayed(() -> adapter.setHighlightedItemId(-1), HIGHLIGHT_DURATION_MS);
    });

    highlightItemId = -1; // One-time behavior
  }
  //endregion

  //region Swipe-to-Delete
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
        return false; // No drag-and-drop
      }

      @Override
      public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
          return;
        }

        KitItem item = adapter.getItemAt(position);

        // Remove from UI by submitting a new list copy
        List<KitItem> before = new ArrayList<>(adapter.getCurrentList());
        List<KitItem> after = new ArrayList<>(before);
        after.remove(position);
        adapter.submitList(after);

        // UNDO snackbar
        Snackbar.make(recyclerView, R.string.item_removed, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, v -> {
              // Restore item (submit a new list)
              List<KitItem> restored = new ArrayList<>(adapter.getCurrentList());
              restored.add(position, item);
              adapter.submitList(restored);

              recyclerView.scrollToPosition(position);
              showToast(getString(R.string.item_restored));
            })
            .addCallback(new Snackbar.Callback() {
              @Override
              public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                  // Cancel notifications before delete
                  cancelItemNotifications(item);

                  repository.deleteItem(item, () -> {
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
    int expCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()),
        "ITEM_EXP");
    NotificationScheduler.cancel(this, AlertReceiver.class, expCode);

    int zeroCode = NotificationScheduler.generateRequestCode(String.valueOf(item.getItemID()),
        "ITEM_ZERO");
    NotificationScheduler.cancel(this, AlertReceiver.class, zeroCode);
  }
  //endregion

  //region Search Menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_kit_items, menu);

    MenuItem searchItem = menu.findItem(R.id.action_search);

    if (searchItem == null) {
      return true;
    }

    SearchView searchView = (SearchView) searchItem.getActionView();
    if (searchView == null) {
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
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        String q = (newText == null) ? "" : newText.trim();

        if (q.isEmpty()) {
          adapter.setItems(fullItemList);

          boolean hasItems = fullItemList != null && !fullItemList.isEmpty();
          applyListVisibility(hasItems);
          applyEmptyStateMode(true);
        } else {
          repository.searchItemsInKit(kitID, q, results -> {
            adapter.setItems(results);

            boolean hasResults = results != null && !results.isEmpty();
            applyListVisibility(hasResults);

            if (!hasResults) {
              applyEmptyStateMode(true);
            }
          });
        }
        return true;
      }
    });

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    if (item.getItemId() == R.id.action_edit_kit) {
      Intent intent = new Intent(this, KitEditActivity.class);
      intent.putExtra(KitEditActivity.EXTRA_KIT_ID, kitID);
      startActivity(intent);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
  //endregion

  //region Helpers
  private void launchAddItem() {
    Intent intent = new Intent(this, KitItemEditActivity.class);
    intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, kitID);
    startActivity(intent);
  }

  private void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30
        ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
  }
}
