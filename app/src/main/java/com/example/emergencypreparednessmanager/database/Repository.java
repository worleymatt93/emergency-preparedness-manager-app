package com.example.emergencypreparednessmanager.database;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.dao.CategoryDAO;
import com.example.emergencypreparednessmanager.dao.KitDAO;
import com.example.emergencypreparednessmanager.dao.KitItemDAO;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.example.emergencypreparednessmanager.ui.receivers.AlertReceiver;
import com.example.emergencypreparednessmanager.util.AppConstants;
import com.example.emergencypreparednessmanager.util.NotificationScheduler;
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
   * Retrieves all items across all kits.
   *
   * @param callback receives the list on the main thread
   */
  public void getAllItems(Consumer<List<KitItem>> callback) {
    databaseExecutor.execute(() -> {
      List<KitItem> items = kitItemDAO.getAllItems();
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

  //region Notifications (Reschedule after reboot / app update)
  /**
   * Reschedules all kit and item notifications from database state.
   * <p>
   * Intended for BOOT_COMPLETED / MY_PACKAGE_REPLACED receivers.
   * Cancels first to prevent duplicates, then schedules according to the same rules used in the UI.
   *
   * @param context any context; applicationContext will be used internally
   */
  public void rescheduleAllNotifications(Context context) {
    final Context appContext = context.getApplicationContext();

    databaseExecutor.execute(() -> {
      // Respect global setting
      if (!NotificationScheduler.areNotificationsEnabled(appContext)) {
        return;
      }

      List<Kit> kits = kitDAO.getAllKits();
      List<KitItem> items = kitItemDAO.getAllItems();

      // 1) Kit reminders
      if (kits != null) {
        for (Kit kit : kits) {
          if (kit == null || !kit.isNotificationsEnabled()) continue;

          String freq = kit.getNotificationFrequency();
          if (freq == null || freq.trim().isEmpty()) continue;

          int requestCode = NotificationScheduler.generateRequestCode(
              String.valueOf(kit.getKitID()),
              NotificationScheduler.TYPE_KIT_REMINDER
          );

          NotificationScheduler.cancel(appContext, AlertReceiver.class, requestCode);

          String title = appContext.getString(R.string.kit_notification_title, kit.getKitName());
          String message = appContext.getString(R.string.kit_notification_message, kit.getKitName());

          NotificationScheduler.scheduleKitFrequency(
              appContext,
              AlertReceiver.class,
              title,
              message,
              freq,
              requestCode,
              String.valueOf(kit.getKitID())
          );
        }
      }

      // 2) Item alarms (expired, expires soon, zero)
      if (items != null) {
        for (KitItem item : items) {
          if (item == null) continue;

          String itemId = String.valueOf(item.getItemID());
          String kitId = String.valueOf(item.getKitID());

          // Expiration alarms
          int expiredCode = NotificationScheduler.generateRequestCode(
              itemId, NotificationScheduler.TYPE_ITEM_EXPIRED
          );
          int soonCode = NotificationScheduler.generateRequestCode(
              itemId, NotificationScheduler.TYPE_ITEM_EXPIRES_SOON
          );

          NotificationScheduler.cancel(appContext, AlertReceiver.class, expiredCode);
          NotificationScheduler.cancel(appContext, AlertReceiver.class, soonCode);

          if (item.isExpirationRemindersEnabled()) {
            String expDate = item.getExpirationDate();
            if (!TextUtils.isEmpty(expDate)) {
              String title = appContext.getString(R.string.item_expiration_title, item.getItemName());

              // A) EXPIRED (day-of)
              String expiredMsg = appContext.getString(
                  R.string.item_expired_message,
                  item.getItemName(),
                  expDate
              );

              NotificationScheduler.schedule(
                  appContext,
                  AlertReceiver.class,
                  NotificationScheduler.TYPE_ITEM_EXPIRED,
                  title,
                  expiredMsg,
                  expDate,
                  expiredCode,
                  null,
                  null,
                  itemId,
                  kitId,
                  null
              );

              // B) EXPIRES SOON (daysBefore)
              int daysBefore = Math.max(0, item.getNotifyDaysBefore());
              if (daysBefore > 0) {
                String fireDate = NotificationScheduler.subtractDays(expDate, daysBefore);
                if (!TextUtils.isEmpty(fireDate)) {
                  String soonMsg = appContext.getResources().getQuantityString(
                      R.plurals.item_expires_soon_message,
                      daysBefore,
                      item.getItemName(),
                      daysBefore,
                      expDate
                  );

                  NotificationScheduler.schedule(
                      appContext,
                      AlertReceiver.class,
                      NotificationScheduler.TYPE_ITEM_EXPIRES_SOON,
                      title,
                      soonMsg,
                      fireDate,
                      soonCode,
                      null,
                      null,
                      itemId,
                      kitId,
                      daysBefore
                  );
                }
              }
            }
          }

          // Zero alarm
          int zeroCode = NotificationScheduler.generateRequestCode(
              itemId, NotificationScheduler.TYPE_ITEM_ZERO
          );
          NotificationScheduler.cancel(appContext, AlertReceiver.class, zeroCode);

          if (item.isNotifyOnZero() && item.getQuantity() <= 0) {
            String title = appContext.getString(R.string.item_zero_title, item.getItemName());
            String message = appContext.getString(R.string.item_zero_message, item.getItemName());

            long trigger = System.currentTimeMillis() + AppConstants.ZERO_QUANTITY_ALERT_DELAY_MS;

            NotificationScheduler.scheduleAtMillis(
                appContext,
                AlertReceiver.class,
                NotificationScheduler.TYPE_ITEM_ZERO,
                title,
                message,
                trigger,
                zeroCode,
                itemId,
                kitId,
                null
            );
          }
        }
      }
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
