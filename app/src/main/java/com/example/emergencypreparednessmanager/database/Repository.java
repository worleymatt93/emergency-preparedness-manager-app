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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Repository providing access to Kit, KitItem, and Category data.
 * Handles asynchronous database operations and posts results to the main thread.
 */
public class Repository {

    // ------------------- EXECUTORS -------------------

    private static final int NUMBER_OF_THREADS = 4;
    private static final ExecutorService databaseExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ------------------- DAOs -------------------

    private final KitDAO mKitDAO;
    private final KitItemDAO mKitItemDAO;
    private final CategoryDAO mCategoryDAO;

    // ------------------- CONSTRUCTOR -------------------

    /**
     * Initializes the DAO instances from the Room database.
     *
     * @param application   Application context
     */
    public Repository(Application application) {
        DatabaseBuilder db = DatabaseBuilder.getDatabase(application);
        mKitDAO = db.kitDAO();
        mKitItemDAO = db.kitItemDAO();
        mCategoryDAO = db.categoryDAO();
    }

    // ------------------- KITS -------------------

    /**
     * Retrieves all kits asynchronously.
     *
     * @param callback      receives the list of kits on the main thread
     */
    public void getAllKits(Consumer<List<Kit>> callback) {
        databaseExecutor.execute(() -> {
            List<Kit> kits = mKitDAO.getAllKits();
            mainHandler.post(() -> callback.accept(kits));
        });
    }

    /**
     * Retrieves a kit by ID asynchronously.
     *
     * @param kitID         ID of the kit
     * @param callback      receives the Kit on the main thread
     */

    public void getKitById(int kitID, Consumer<Kit> callback) {
        databaseExecutor.execute(() -> {

            // Fetch kit by ID in background
            Kit kit = mKitDAO.getKitByID(kitID);

            // Post result to main thread
            mainHandler.post(() -> callback.accept(kit));
        });
    }

    /**
     * Inserts a kit into the database asynchronously.
     *
     * @param kit           Kit to insert
     * @param callback      Runnable called on the main thread after insertion
     */
    public void insert(Kit kit, Consumer<Long> callback) {
        databaseExecutor.execute(() -> {
            long id = mKitDAO.insert(kit);
            mainHandler.post(() -> {
                if (callback != null) callback.accept(id);
            });
        });
    }

    /**
     * Updates an existing kit asynchronously.
     *
     * @param kit           Kit to update
     * @param callback      Runnable called on the main thread after update
     */
    public void update(Kit kit, Runnable callback) {
        databaseExecutor.execute(() -> {
            mKitDAO.update(kit);
            if (callback != null) mainHandler.post(callback);
        });
    }

    /**
     * Deletes a kit asynchronously.
     *
     * @param kit           Kit to delete
     * @param callback      Runnable called on the main thread after deletion
     */
    public void delete(Kit kit, Runnable callback) {
        databaseExecutor.execute(() -> {
            mKitDAO.delete(kit);
            if (callback != null) mainHandler.post(callback);
        });
    }

    public void getKitsWithNotificationsEnabled(Consumer<List<Kit>> callback) {
        databaseExecutor.execute(() -> {
            List<Kit> kits = mKitDAO.getKitsWithNotificationsEnabled();
            mainHandler.post(() -> callback.accept(kits));
        });
    }

    public void getKitsByFrequency(String frequency, Consumer<List<Kit>> callback) {
        databaseExecutor.execute(() -> {
            List<Kit> kits = mKitDAO.getKitsByFrequency(frequency);
            mainHandler.post(() -> callback.accept(kits));
        });
    }

    public void countItemsForKit(int kitID, Consumer<Integer> callback) {
        databaseExecutor.execute(() -> {
            int count = mKitItemDAO.countItemsForKit(kitID);
            mainHandler.post(() -> callback.accept(count));
        });
    }

    public void getKitIdsThatHaveItems(Consumer<List<Integer>> callback) {
        databaseExecutor.execute(() -> {
            List<Integer> ids = mKitItemDAO.getKitIdsThatHaveItems();
            mainHandler.post(() -> callback.accept(ids));
        });
    }

    // ------------------- KIT ITEMS -------------------

    public void getItemsForKit(int kitID, Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> items = mKitItemDAO.getItemsForKit(kitID);
            mainHandler.post(() -> callback.accept(items));
        });
    }

    public void getItemById(int itemID, Consumer<KitItem> callback) {
        databaseExecutor.execute(() -> {
            KitItem item = mKitItemDAO.getItemById(itemID);
            mainHandler.post(() -> callback.accept(item));
        });
    }

    public void searchItemsInKit(int kitID, String query, Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> results = mKitItemDAO.searchItemsInKit(kitID, query);
            mainHandler.post(() -> callback.accept(results));
        });
    }

    public void searchKits(String query, Consumer<List<Kit>> callback) {
        databaseExecutor.execute(() -> {
            List<Kit> kits = mKitDAO.searchKits(query);
            mainHandler.post(() -> callback.accept(kits));
        });
    }

    public void searchAllItems(String query, Consumer<List<ItemSearchRow>> callback) {
        databaseExecutor.execute(() -> {
            List<ItemSearchRow> results = mKitItemDAO.searchAllItems(query);
            mainHandler.post(() -> callback.accept(results));
        });
    }

    /**
     * Inserts a kit item asynchronously.
     *
     * @param item          Kit Item to insert
     * @param callback      Runnable called on the main thread after insertion
     */
    public void insert(KitItem item, Consumer<Long> callback) {
        databaseExecutor.execute(() -> {
            long id = mKitItemDAO.insert(item);
            mainHandler.post(() -> {
                if (callback != null) callback.accept(id);
            });
        });
    }

    /**
     * Updates a kit item asynchronously.
     *
     * @param item          Kit Item to update
     * @param callback      Runnable called on the main thread after update
     */
    public void update(KitItem item, Runnable callback) {
        databaseExecutor.execute(() -> {
            mKitItemDAO.update(item);
            if (callback != null) mainHandler.post(callback);
        });
    }

    /**
     * Deletes a kit item asynchronously.
     *
     * @param item          Kit Item to delete
     * @param callback      Runnable called on the main thread after deletion
     */
    public void delete(KitItem item, Runnable callback) {
        databaseExecutor.execute(() -> {
            mKitItemDAO.delete(item);
            if (callback != null) mainHandler.post(callback);
        });
    }

    public void getItemsWithExpirationRemindersEnabled(Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> items = mKitItemDAO.getItemsWithExpirationRemindersEnabled();
            mainHandler.post(() -> callback.accept(items));
        });
    }

    public void getItemsWithNotifyOnZeroEnabled(Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> items = mKitItemDAO.getItemsWithNotifyOnZeroEnabled();
            mainHandler.post(() -> callback.accept(items));
        });
    }

    public void getZeroQuantityItemsNeedingNotification(Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> items = mKitItemDAO.getZeroQuantityItemsNeedingNotification();
            mainHandler.post(() -> callback.accept(items));
        });
    }

    public void updateItemQuantity(int itemID, int newQuantity, Runnable callback) {
        databaseExecutor.execute(() -> {
            mKitItemDAO.updateQuantity(itemID, newQuantity);
            if (callback != null) mainHandler.post(callback);
        });
    }

    public void adjustItemQuantity(int itemID, int delta, Consumer<KitItem> callback) {
        databaseExecutor.execute(() -> {
            KitItem updated = mKitItemDAO.adjustQuantityAndGet(itemID, delta);
            mainHandler.post(() -> callback.accept(updated));
        });
    }

    // ------------------- CATEGORIES -------------------

    public void getAllCategories(Consumer<List<Category>> callback) {
        databaseExecutor.execute(() -> {
            List<Category> categories = mCategoryDAO.getAllCategories();
            mainHandler.post(() -> callback.accept(categories));
        });
    }

    public void insert(Category category, Consumer<Long> callback) {
        databaseExecutor.execute(() -> {
            long id = mCategoryDAO.insert(category);
            mainHandler.post(() -> callback.accept(id));
        });
    }

    // ------------------- SEARCH -------------------

    public void searchItems(String query, Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> results = mKitItemDAO.searchItems(query);
            mainHandler.post(() -> callback.accept(results));
        });
    }

    // ------------------- REPORTS -------------------

    public void getItemsExpiringBetween(String startDate, String endDate, Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> results = mKitItemDAO.getItemsExpiringBetween(startDate, endDate);
            mainHandler.post(() -> callback.accept(results));
        });
    }

    public void getLowStockItems(int threshold, Consumer<List<KitItem>> callback) {
        databaseExecutor.execute(() -> {
            List<KitItem> results = mKitItemDAO.getLowStockItems(threshold);
            mainHandler.post(() -> callback.accept(results));
        });
    }
}
