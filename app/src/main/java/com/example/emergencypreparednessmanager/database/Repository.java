package com.example.emergencypreparednessmanager.database;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import com.example.emergencypreparednessmanager.dao.CategoryDAO;
import com.example.emergencypreparednessmanager.dao.KitDAO;
import com.example.emergencypreparednessmanager.dao.KitItemDAO;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.entities.KitItem;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Single source of truth for data access in the app.
 * <p>
 * Encapsulates all DAO interactions, runs database operations off the main thread, and delivers
 * results back to the main thread via callbacks.
 */
public class Repository {

  //region Executors
  private static final int NUMBER_OF_THREADS = 4;
  private static final ExecutorService databaseExecutor =
      Executors.newFixedThreadPool(NUMBER_OF_THREADS);
  private static final Handler mainHandler = new Handler(Looper.getMainLooper());
  //endregion

  //region DAOs
  private final KitDAO kitDAO;
  private final KitItemDAO kitItemDAO;
  private final CategoryDAO categoryDAO;
  //endregion

  //region Constructor

  /**
   * Initializes the repository with DAO references from the Room database.
   *
   * @param application application context
   */
  public Repository(Application application) {
    DatabaseBuilder db = DatabaseBuilder.getDatabase(application);
    kitDAO = db.kitDAO();
    kitItemDAO = db.kitItemDAO();
    categoryDAO = db.categoryDAO();
  }
  //endregion

  //region Kits

  /**
   * Retrieves all kits asynchronously.
   *
   * @param callback receives the list of kits on the main thread
   */
  public void getAllKits(Consumer<List<Kit>> callback) {
    databaseExecutor.execute(() -> {
      List<Kit> kits = kitDAO.getAllKits();
      mainHandler.post(() -> callback.accept(kits));
    });
  }

  /**
   * Retrieves a single kit by ID asynchronously.
   *
   * @param kitID    kit ID to find
   * @param callback receives the kit on the main thread (null if not found)
   */
  public void getKitById(int kitID, Consumer<Kit> callback) {
    databaseExecutor.execute(() -> {
      Kit kit = kitDAO.getKitByID(kitID);
      mainHandler.post(() -> callback.accept(kit));
    });
  }

  /**
   * Inserts a new kit asynchronously.
   *
   * @param kit      kit to insert
   * @param callback receives the new row ID on the main thread
   */
  public void insertKit(Kit kit, Consumer<Long> callback) {
    databaseExecutor.execute(() -> {
      long id = kitDAO.insert(kit);
      mainHandler.post(() -> callback.accept(id));
    });
  }

  /**
   * Updates an existing kit asynchronously.
   *
   * @param kit      kit with updated values
   * @param callback called on the main thread after update (optional)
   */
  public void updateKit(Kit kit, Runnable callback) {
    databaseExecutor.execute(() -> {
      kitDAO.update(kit);
      if (callback != null) {
        mainHandler.post(callback);
      }
    });
  }

  /**
   * Deletes a kit asynchronously.
   *
   * @param kit      kit to delete
   * @param callback called on the main thread after deletion (optional)
   */
  public void deleteKit(Kit kit, Runnable callback) {
    databaseExecutor.execute(() -> {
      kitDAO.delete(kit);
      if (callback != null) {
        mainHandler.post(callback);
      }
    });
  }

  /**
   * Searches kits by name or location.
   *
   * @param query    search text
   * @param callback receives matching kits on main thread
   */
  public void searchKits(String query, Consumer<List<Kit>> callback) {
    databaseExecutor.execute(() -> {
      List<Kit> kits = kitDAO.searchKits(query);
      mainHandler.post(() -> callback.accept(kits));
    });
  }
  //endregion

  //region Kit Items

  /**
   * Retrieves all items in a specific kit.
   *
   * @param kitID    kit ID
   * @param callback receives the list on the main thread
   */
  public void getItemsForKit(int kitID, Consumer<List<KitItem>> callback) {
    databaseExecutor.execute(() -> {
      List<KitItem> items = kitItemDAO.getItemsForKit(kitID);
      mainHandler.post(() -> callback.accept(items));
    });
  }

  /**
   * Retrieves a single kit item by ID.
   *
   * @param itemID   item ID
   * @param callback receives the item on the main thread (null if not found)
   */
  public void getItemById(int itemID, Consumer<KitItem> callback) {
    databaseExecutor.execute(() -> {
      KitItem item = kitItemDAO.getItemById(itemID);
      mainHandler.post(() -> callback.accept(item));
    });
  }

  /**
   * Inserts a new kit item asynchronously.
   *
   * @param item     item to insert
   * @param callback receives the new row ID on the main thread
   */
  public void insertItem(KitItem item, Consumer<Long> callback) {
    databaseExecutor.execute(() -> {
      long id = kitItemDAO.insert(item);
      mainHandler.post(() -> callback.accept(id));
    });
  }

  /**
   * Updates an existing kit item asynchronously.
   *
   * @param item     item with updated values
   * @param callback called on the main thread after update (optional)
   */
  public void updateItem(KitItem item, Runnable callback) {
    databaseExecutor.execute(() -> {
      kitItemDAO.update(item);
      if (callback != null) {
        mainHandler.post(callback);
      }
    });
  }

  /**
   * Deletes a kit item asynchronously.
   *
   * @param item     item to delete
   * @param callback called on the main thread after deletion (optional)
   */
  public void deleteItem(KitItem item, Runnable callback) {
    databaseExecutor.execute(() -> {
      kitItemDAO.delete(item);
      if (callback != null) {
        mainHandler.post(callback);
      }
    });
  }

  /**
   * Adjusts quantity by delta (clamps to ≥ 0) and returns the updated item.
   *
   * @param itemID   item ID
   * @param delta    amount to add (positive or negative)
   * @param callback receives the updated item on main thread
   */
  public void adjustItemQuantity(int itemID, int delta, Consumer<KitItem> callback) {
    databaseExecutor.execute(() -> {
      KitItem updated = kitItemDAO.adjustQuantityAndGet(itemID, delta);
      mainHandler.post(() -> callback.accept(updated));
    });
  }

  /**
   * Searches items within a specific kit.
   *
   * @param kitID    kit ID
   * @param query    search text
   * @param callback receives matching items on main thread
   */
  public void searchItemsInKit(int kitID, String query, Consumer<List<KitItem>> callback) {
    databaseExecutor.execute(() -> {
      List<KitItem> results = kitItemDAO.searchItemsInKit(kitID, query);
      mainHandler.post(() -> callback.accept(results));
    });
  }

  /**
   * Retrieves the set of all kit IDs that contain at least one item.
   * <p>
   * Used for bulk delete protection (e.g., swipe-to-delete in kit list). Returns a Set<Integer> for
   * fast contains() checks.
   *
   * @param callback receives the set of non-deletable kit IDs on the main thread
   */
  public void getNonDeletableKitIds(Consumer<Set<Integer>> callback) {
    databaseExecutor.execute(() -> {
      List<Integer> ids = kitItemDAO.getKitIdsWithAtLeastOneItem();
      Set<Integer> set = ids != null ? new HashSet<>(ids) : new HashSet<>();
      mainHandler.post(() -> callback.accept(set));
    });
  }
  //endregion

  //region Categories

  /**
   * Retrieves all categories asynchronously.
   *
   * @param callback receives the list on main thread
   */
  public void getAllCategories(Consumer<List<Category>> callback) {
    databaseExecutor.execute(() -> {
      List<Category> categories = categoryDAO.getAllCategories();
      mainHandler.post(() -> callback.accept(categories));
    });
  }

  /**
   * Inserts a new category asynchronously.
   *
   * @param category category to insert
   * @param callback receives the new row ID on main thread
   */
  public void insertCategory(Category category, Consumer<Long> callback) {
    databaseExecutor.execute(() -> {
      long id = categoryDAO.insert(category);
      mainHandler.post(() -> callback.accept(id));
    });
  }
  //endregion

  //region Search

  /**
   * Searches items globally across all kits (denormalized results).
   *
   * @param query    search text
   * @param callback receives denormalized search rows on main thread
   */
  public void searchAllItems(String query, Consumer<List<ItemSearchRow>> callback) {
    databaseExecutor.execute(() -> {
      List<ItemSearchRow> results = kitItemDAO.searchAllItems(query);
      mainHandler.post(() -> callback.accept(results));
    });
  }
  //endregion

  //region Reports

  /**
   * Retrieves full inventory report rows (denormalized).
   *
   * @param callback receives the list on main thread
   */
  public void getInventoryReportRows(Consumer<List<ItemSearchRow>> callback) {
    databaseExecutor.execute(() -> {
      List<ItemSearchRow> rows = kitItemDAO.getInventoryReportRows();
      mainHandler.post(() -> callback.accept(rows));
    });
  }
  //endregion
}
