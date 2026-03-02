package com.example.emergencypreparednessmanager.ui.activities;

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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.ui.adapters.KitAdapter;
import com.example.emergencypreparednessmanager.ui.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Activity displaying the list of kits with search, add, and swipe-to-delete functionality.
 * <p>
 * Features:
 * <ul>
 *   <li>RecyclerView of kits with swipe-to-delete (blocked if kit has items)</li>
 *   <li>FAB and empty-state "Create First Kit" button</li>
 *   <li>Search bar with live filtering</li>
 *   <li>Undo via Snackbar on delete</li>
 * </ul>
 */
public class KitListActivity extends AppCompatActivity {

  //region Constants
  private static final String TAG = "KitListActivity";
  //endregion
  private final Set<Integer> nonDeletableKitIds = new HashSet<>();
  //region Fields
  private MaterialToolbar toolbar;
  private RecyclerView recyclerView;
  private View emptyStateLayout;
  private MaterialTextView emptyTitle, emptySubtitle;
  private MaterialButton btnCreateFirstKit;
  private FloatingActionButton fab;
  private Repository repository;
  private KitAdapter kitAdapter;
  private List<Kit> fullKitList;
  private boolean searchExpanded = false;
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_kit_list);

    repository = new Repository(getApplication());

    bindViews();
    setupInsets();
    setupToolbar();
    setupRecyclerView();
    setupButtons();

    loadKits();
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadKits(); // Refresh on return (e.g., after edit/add)
  }
  //endregion

  //region Setup
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
  //endregion

  //region Data Loading
  private void loadKits() {
    repository.getAllKits(kits -> runOnUiThread(() -> {
      fullKitList = kits;

      kitAdapter.setKits(kits);

      boolean hasKits = kits != null && !kits.isEmpty();
      applyListVisibility(hasKits);

      if (!searchExpanded) {
        applyEmptyStateMode(false);
        fab.setVisibility(hasKits ? View.VISIBLE : View.GONE);
      } else {
        applyEmptyStateMode(true);
        fab.setVisibility(View.GONE);
      }
      precomputeNonDeletableKits();
    }));
  }

  private void applyListVisibility(boolean showList) {
    recyclerView.setVisibility(showList ? View.VISIBLE : View.GONE);
    emptyStateLayout.setVisibility(showList ? View.GONE : View.VISIBLE);
  }

  private void applyEmptyStateMode(boolean searching) {
    if (searching) {
      emptyTitle.setText(R.string.no_matching_kits);
      emptySubtitle.setText(R.string.try_different_search);
      btnCreateFirstKit.setVisibility(View.GONE);
      fab.setVisibility(View.GONE);
    } else {
      emptyTitle.setText(R.string.no_kits_yet);
      emptySubtitle.setText(R.string.create_your_first_kit_to_start_tracking_supplies);
      boolean hasKits = fullKitList != null && !fullKitList.isEmpty();
      btnCreateFirstKit.setVisibility(hasKits ? View.GONE : View.VISIBLE);
    }
  }

  private void precomputeNonDeletableKits() {
    nonDeletableKitIds.clear();

    repository.getNonDeletableKitIds(ids -> {
      nonDeletableKitIds.addAll(ids);
      Log.d(TAG, "Loaded " + ids.size() + "non-deletable kid IDs");
    });
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

        Kit kit = kitAdapter.getKitAt(position);

        // Defensive: if protection hasn't loaded yet, snap back
        if (nonDeletableKitIds.isEmpty() && !kitAdapter.getCurrentList().isEmpty()) {
          kitAdapter.notifyItemChanged(position);
          showToast(getString(R.string.loading_delete_protection));
          return;
        }

        // Block deletion if kit has items
        if (nonDeletableKitIds.contains(kit.getKitID())) {
          kitAdapter.notifyItemChanged(position);
          showToast(getString(R.string.cannot_delete_kit_with_items));
          return;
        }

        // Remove from UI by submitting a new list copy
        List<Kit> before = new ArrayList<>(kitAdapter.getCurrentList());
        List<Kit> after = new ArrayList<>(before);
        after.remove(position);
        kitAdapter.submitList(after);

        // Show undo Snackbar
        Snackbar.make(recyclerView, R.string.kit_removed, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, v -> {
              List<Kit> restored = new ArrayList<>(kitAdapter.getCurrentList());
              restored.add(position, kit);
              kitAdapter.submitList(restored);
              recyclerView.scrollToPosition(position);
              showToast(getString(R.string.kit_restored));
            })
            .addCallback(new Snackbar.Callback() {
              @Override
              public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event != DISMISS_EVENT_ACTION) {
                  // Cancel notifications BEFORE deleting (safe even if none exist)
                  cancelKitNotifications(kit);

                  // Then commit deletion
                  repository.deleteKit(kit, () -> {
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
  //endregion

  //region Search Menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_kit_list, menu);

    MenuItem searchItem = menu.findItem(R.id.action_search);
    if (searchItem == null) {
      return true;
    }

    SearchView searchView = (SearchView) searchItem.getActionView();
    if (searchView == null) {
      return true;
    }

    searchView.setQueryHint(getString(R.string.search_kits));

    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
        searchExpanded = true;
        applyEmptyStateMode(true);
        fab.setVisibility(View.GONE);
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
        searchExpanded = false;
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
          kitAdapter.setKits(fullKitList);
          boolean hasKits = fullKitList != null && !fullKitList.isEmpty();
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
  //endregion

  //region Helpers
  private void launchAddKit() {
    Intent intent = new Intent(this, KitEditActivity.class);
    startActivity(intent);
  }

  private void cancelKitNotifications(Kit kit) {
    int requestCode = NotificationScheduler.generateRequestCode(
        String.valueOf(kit.getKitID()), "KIT");
    NotificationScheduler.cancel(this, AlertReceiver.class, requestCode);
  }

  private void showToast(String message) {
    Toast.makeText(this, message, message.length() >= 30 ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
        .show();
  }
  //endregion
}
