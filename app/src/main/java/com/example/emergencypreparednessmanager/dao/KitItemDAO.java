package com.example.emergencypreparednessmanager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
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

    // ------------------- RELATIONSHIP -------------------

    @Query("SELECT * FROM KitItems WHERE kitID = :kitID ORDER BY itemID ASC")
    List<KitItem> getItemsForKit(int kitID);

    // ------------------- SEARCH -------------------

    @Query("SELECT * FROM KitItems WHERE itemName LIKE '%' || :query || '%' OR notes LIKE '%' " +
            "|| :query || '%' ORDER BY itemName ASC")
    List<KitItem> searchItems(String query);


    // ------------------- REPORT QUERIES -------------------

    // Expiring soon report
    @Query("SELECT * FROM KitItems WHERE expirationDate BETWEEN :startDate AND :endDate ORDER BY expirationDate ASC")
    List<KitItem> getItemsExpiringBetween(String startDate, String endDate);

    // Low stock report
    @Query("SELECT * FROM KitItems WHERE quantity <= :threshold ORDER BY quantity ASC")
    List<KitItem> getLowStockItems(int threshold);
}
