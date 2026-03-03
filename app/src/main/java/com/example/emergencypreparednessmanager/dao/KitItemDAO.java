package com.example.emergencypreparednessmanager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.example.emergencypreparednessmanager.entities.KitItem;
import java.util.List;

/**
 * Room DAO for {@link KitItem} entities.
 * <p>
 * Provides CRUD operations, per-kit item retrieval, item counting, and queries used for
 * delete-protection logic.
 */
@Dao
public interface KitItemDAO {

  //region CRUD Operations
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(KitItem item);

  @Update
  void update(KitItem item);

  @Delete
  void delete(KitItem item);
  //endregion

  //region Per-Kit Queries

  /**
   * All items in a given kit, ordered by ID.
   *
   * @param kitID kit ID
   * @return items belonging to the kit, ordered by ID
   */
  @Query("SELECT * FROM KitItems WHERE kitID = :kitID ORDER BY itemID ASC")
  List<KitItem> getItemsForKit(int kitID);

  /**
   * Number of items in a given kit. Used for delete protection.
   *
   * @param kitID kit ID
   * @return number of items in a kit
   */
  @Query("SELECT COUNT(*) FROM KitItems WHERE kitID = :kitID")
  int countItemsForKit(int kitID);
  //endregion

  //region Item-level Operations

  /**
   * Retrieves a single item by ID.
   *
   * @param itemID item ID
   * @return matching item, or null if not found
   */
  @Query("SELECT * FROM KitItems WHERE itemID = :itemID LIMIT 1")
  KitItem getItemById(int itemID);


  /**
   * Adjusts quantity by delta, clamping to ≥ 0.
   *
   * @param itemID item ID
   * @param delta  amount to add (positive or negative)
   */
  @Query("UPDATE KitItems SET quantity = CASE " +
      "WHEN quantity + :delta < 0 THEN 0 " +
      "ELSE quantity + :delta END WHERE itemID = :itemID")
  void adjustQuantity(int itemID, int delta);

  /**
   * Adjusts quantity by delta and returns the refreshed entity. Convenience wrapper for UI code
   * needing the updated object immediately.
   *
   * @param itemID item ID
   * @param delta  amount to add (positive or negative)
   * @return updated item, or null if not found
   */
  @Transaction
  default KitItem adjustQuantityAndGet(int itemID, int delta) {
    adjustQuantity(itemID, delta);
    return getItemById(itemID);
  }
  //endregion

  //region Search Queries
  @Query("SELECT * FROM KitItems")
  List<KitItem> getAllItems();

  /**
   * Search items within a specific kit.
   *
   * @param kitID kit ID
   * @param query search text (partial match)
   * @return matching items in the kit, ordered by name
   */
  @Query("SELECT * FROM KitItems " +
      "WHERE kitID = :kitID AND (" +
      "itemName LIKE '%' || :query || '%' " +
      "OR CAST(quantity AS TEXT) LIKE '%' || :query || '%' " +
      "OR expirationDate LIKE '%' || :query || '%' " +
      ") ORDER BY itemName COLLATE NOCASE ASC")
  List<KitItem> searchItemsInKit(int kitID, String query);

  /**
   * Global search including kit name and category name (denormalized for display).
   *
   * @param query search text (partial match on item, quantity, expiration, kit, or category)
   * @return denormalized search rows
   */
  @Query(
      "SELECT " +
          "ki.itemID, ki.kitID, ki.itemName, ki.quantity, ki.expirationDate, " +
          "k.kitName, c.categoryName " +
          "FROM KitItems ki " +
          "JOIN Kits k ON k.kitID = ki.kitID " +
          "LEFT JOIN Categories c ON c.categoryID = ki.categoryID " +
          "WHERE ki.itemName LIKE '%' || :query || '%' " +
          "OR CAST(ki.quantity AS TEXT) LIKE '%' || :query || '%' " +
          "OR ki.expirationDate LIKE '%' || :query || '%' " +
          "OR k.kitName LIKE '%' || :query || '%' " +
          "OR c.categoryName LIKE '%' || :query || '%' " +
          "ORDER BY ki.itemName COLLATE NOCASE ASC")
  List<ItemSearchRow> searchAllItems(String query);
  //endregion

  //region Report / Summary Queries

  /**
   * RFull denormalized dataset for inventory/export reports
   *
   * @return report rows ordered by item name then kit name (case-insensitive)
   */
  @Query(
      "SELECT " +
          "ki.itemID, ki.kitID, ki.itemName, ki.quantity, ki.expirationDate, " +
          "k.kitName, k.location, c.categoryName " +
          "FROM KitItems ki " +
          "JOIN Kits k ON k.kitID = ki.kitID " +
          "LEFT JOIN Categories c ON c.categoryID = ki.categoryID " +
          "ORDER BY ki.itemName COLLATE NOCASE ASC, k.kitName COLLATE NOCASE ASC")
  List<ItemSearchRow> getInventoryReportRows();
  //endregion

  //region Delete Protection

  /**
   * All kit IDs that have ≥ 1 item. Used to block deletion of non-empty kits.
   *
   * @return kit IDs that have one or more items
   */
  @Query("SELECT DISTINCT kitID FROM KitItems ORDER BY kitID ASC")
  List<Integer> getKitIdsWithAtLeastOneItem();
  //endregion
}
