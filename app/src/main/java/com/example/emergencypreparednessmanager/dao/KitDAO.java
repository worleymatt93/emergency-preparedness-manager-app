package com.example.emergencypreparednessmanager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.emergencypreparednessmanager.entities.Kit;

import java.util.List;

/**
 * Data Access Object for Kit entities.
 * Defines database operations related to emergency kits.
 */
@Dao
public interface KitDAO {


    // ------------------- CRUD OPERATIONS -------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Kit kit);

    @Update
    void update(Kit kit);

    @Delete
    void delete(Kit kit);

    @Query("SELECT * FROM Kits ORDER BY kitID ASC")
    List<Kit> getAllKits();

    @Query("SELECT * FROM Kits WHERE kitID = :id LIMIT 1")
    Kit getKitByID(int id);

    // ------------------- NOTIFICATIONS -------------------

    @Query("SELECT * FROM Kits WHERE notificationsEnabled = 1 ORDER BY kitID ASC")
    List<Kit> getKitsWithNotificationsEnabled();

    @Query("SELECT * FROM Kits WHERE notificationsEnabled = 1 AND notificationFrequency = :frequency ORDER BY kitID ASC")
    List<Kit> getKitsByFrequency(String frequency);
}
