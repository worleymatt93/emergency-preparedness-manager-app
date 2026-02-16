package com.example.emergencypreparednessmanager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.emergencypreparednessmanager.entities.KitItem;

import java.util.List;

/**
 * Data Access Object for KitItem entities.
 * Defines database operations related to items inside kits.
 */
@Dao
public interface KitItemDAO {

    // ------------------- CRUD OPERATIONS -------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(KitItem item);

    @Update
    void update(KitItem item);

    @Delete
    void delete(KitItem item);

    @Query("SELECT * FROM KitItems ORDER BY itemID ASC")
    List<KitItem> getAllItems();

    @Query("SELECT * FROM KitItems WHERE itemID = :itemID LIMIT 1")
    KitItem getItemById(int itemID);

    @Query("UPDATE KitItems SET quantity = :newQuantity WHERE itemID = :itemID")
    void updateQuantity(int itemID, int newQuantity);

    @Query("UPDATE KitItems SET quantity = MAX(:newQuantity, 0) WHERE itemID = :itemID")
    void updateQuantityClamped(int itemID, int newQuantity);

    @Query("UPDATE KitItems " +
            "SET quantity = CASE " +
            "WHEN quantity + :delta < 0 THEN 0 " +
            "ELSE quantity + :delta " +
            "END WHERE itemID = :itemID")
    int adjustQuantity(int itemID, int delta);

    @Transaction
    default KitItem adjustQuantityAndGet(int itemID, int delta) {
        adjustQuantity(itemID, delta);
        return getItemById(itemID);
    }

    // ------------------- RELATIONSHIP -------------------

    @Query("SELECT * FROM KitItems WHERE kitID = :kitID ORDER BY itemID ASC")
    List<KitItem> getItemsForKit(int kitID);

    @Query("SELECT COUNT(*) FROM KitItems WHERE kitID = :kitID")
    int countItemsForKit(int kitID);

    @Query("SELECT DISTINCT kitID FROM KitItems")
    List<Integer> getKitIdsThatHaveItems();

    // ------------------- SEARCH -------------------

    @Query("SELECT * FROM KitItems " +
            "WHERE itemName LIKE '%' || :query || '%' " +
            "OR notes LIKE '%' || :query || '%' " +
            "ORDER BY itemName COLLATE NOCASE ASC")
    List<KitItem> searchItems(String query);

    // ------------------- REPORT QUERIES -------------------

    // Expiring soon report
    @Query("SELECT * FROM KitItems WHERE expirationDate BETWEEN :startDate AND :endDate ORDER BY expirationDate ASC")
    List<KitItem> getItemsExpiringBetween(String startDate, String endDate);

    // Low stock report
    @Query("SELECT * FROM KitItems WHERE quantity <= :threshold ORDER BY quantity ASC")
    List<KitItem> getLowStockItems(int threshold);

    // ------------------- NOTIFICATIONS -------------------

    @Query("SELECT * FROM KitItems WHERE expirationRemindersEnabled = 1 ORDER BY itemID ASC")
    List<KitItem> getItemsWithExpirationRemindersEnabled();

    @Query("SELECT * FROM KitItems WHERE notifyOnZero = 1 ORDER BY itemID ASC")
    List<KitItem> getItemsWithNotifyOnZeroEnabled();

    @Query("SELECT * FROM KitItems WHERE notifyOnZero = 1 AND quantity <= 0 ORDER BY itemID ASC")
    List<KitItem> getZeroQuantityItemsNeedingNotification();
}
