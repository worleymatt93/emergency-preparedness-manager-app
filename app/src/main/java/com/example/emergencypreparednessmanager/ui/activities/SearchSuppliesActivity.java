package com.example.emergencypreparednessmanager.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.Repository;
import com.example.emergencypreparednessmanager.ui.adapters.ItemSearchAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textview.MaterialTextView;
import java.util.Collections;
import java.util.Objects;

/**
 * Activity for global search across all kit items.
 * <p>
 * Features:
 * <ul>
 *   <li>SearchView for live filtering (denormalized results)</li>
 *   <li>Results shown in RecyclerView with click to open kit/item</li>
 *   <li>Empty states for "start typing" and "no results"</li>
 *   <li>Keyboard hiding on search submit or collapse</li>
 * </ul>
 */
public class SearchSuppliesActivity extends BaseActivity {

  //region Constants
  private static final String TAG = "SearchSuppliesActivity";
  //endregion

  //region Fields
  private MaterialToolbar toolbar;
  private RecyclerView recyclerView;
  private View emptyStateLayout;
  private MaterialTextView emptyTitle, emptySubtitle, textResultsCount;
  private SearchView searchView;

  private Repository repository;
  private ItemSearchAdapter adapter;
  //endregion

  //region Lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_search_supplies);

    repository = new Repository(getApplication());

    bindViews();
    setupInsets();
    setupToolbar();
    setupRecyclerView();

    // Default: show "Type to search"
    showStartState();
  }
  //endregion

  //region Setup
  private void bindViews() {
    toolbar = findViewById(R.id.toolbar);
    recyclerView = findViewById(R.id.recyclerView);
    emptyStateLayout = findViewById(R.id.emptyStateLayout);
    emptyTitle = findViewById(R.id.emptyTitle);
    emptySubtitle = findViewById(R.id.emptySubtitle);
    textResultsCount = findViewById(R.id.textResultsCount);
  }

  private void setupToolbar() {
    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    toolbar.setTitle(getString(R.string.search_supplies));
  }

  private void setupInsets() {
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

    NestedScrollView scrollView = findViewById(R.id.scrollView);
    if (scrollView != null) {
      ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
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
    adapter = new ItemSearchAdapter(this, row -> {
      Intent intent = new Intent(SearchSuppliesActivity.this, KitItemsActivity.class);
      intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, row.getKitID());
      intent.putExtra(KitItemsActivity.EXTRA_KIT_NAME, row.getKitName());
      intent.putExtra(KitItemsActivity.EXTRA_HIGHLIGHT_ITEM_ID, row.getItemID());

      if (searchView != null) {
        searchView.clearFocus();
      }
      hideKeyboard();
      startActivity(intent);
    });

    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);
  }
  //endregion

  //region Search Menu
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_search_supplies, menu);

    MenuItem searchItem = menu.findItem(R.id.action_search);
    if (searchItem == null) {
      return true;
    }

    searchView = (SearchView) searchItem.getActionView();
    if (searchView == null) {
      return true;
    }

    searchView.setQueryHint(getString(R.string.search_items));

    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
        clearResultsAndShowStartState();
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
        if (searchView != null) {
          searchView.clearFocus();
        }
        hideKeyboard();
        clearResultsAndShowStartState();
        return true;
      }
    });

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        if (searchView != null) {
          searchView.clearFocus();
        }
        hideKeyboard();
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        String q = newText == null ? "" : newText.trim();

        if (q.isEmpty()) {
          clearResultsAndShowStartState();
          return true;
        }

        repository.searchAllItems(q, results -> {
          adapter.setItems(results);

          int count = (results == null) ? 0 : results.size();
          if (count > 0) {
            textResultsCount.setText(
                getResources().getQuantityString(
                    R.plurals.results_count,
                    count,
                    count
                )
            );
            textResultsCount.setVisibility(View.VISIBLE);
            applyListVisibility(true);
          } else {
            textResultsCount.setVisibility(View.GONE);
            showNoResultsState();
          }
        });
        return true;
      }
    });

    return true;
  }
  //endregion

  //region Empty State
  private void applyListVisibility(boolean showList) {
    recyclerView.setVisibility(showList ? View.VISIBLE : View.GONE);
    emptyStateLayout.setVisibility(showList ? View.GONE : View.VISIBLE);
  }

  private void clearResultsAndShowStartState() {
    adapter.setItems(Collections.emptyList());
    textResultsCount.setVisibility(View.GONE);
    showStartState();
  }

  private void showStartState() {
    emptyTitle.setText(R.string.search_supplies);
    emptySubtitle.setText(R.string.type_to_search);
    applyListVisibility(false);
  }

  private void showNoResultsState() {
    emptyTitle.setText(R.string.no_matching_items);
    emptySubtitle.setText(R.string.try_different_search);
    applyListVisibility(false);
  }
  //endregion

  //region Helpers
  private void hideKeyboard() {
    View view = getCurrentFocus();
    if (view == null) {
      view = findViewById(R.id.main);
    }
    if (view == null) {
      return;
    }

    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }
  //endregion
}
