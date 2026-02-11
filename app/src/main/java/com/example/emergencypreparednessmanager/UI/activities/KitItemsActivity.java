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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.adapters.KitItemAdapter;
import com.example.emergencypreparednessmanager.database.Repository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        adapter = new KitItemAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void launchAddItem() {
        // TODO: Replace with KitItemEditActivity
        showToast(getString(R.string.todo_add_item_screen));
    }

    private void setupFab() {
        fab.setOnClickListener(v -> {
            // TODO: Replace with KitItemEditActivity
            showToast(getString(R.string.todo_add_item_screen));

            // Example later:
            // fab.setOnClickListener(v -> launchAddItem();
            // btnAddFirstItem.setOnClickListener(v -> launchAddItem());
        });
    }

    private void loadItems() {
        repository.getItemsForKit(kitID, items -> {
            adapter.setItems(items);

            int count = (items == null) ? 0 : items.size();
            boolean empty = count == 0;

            emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

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
