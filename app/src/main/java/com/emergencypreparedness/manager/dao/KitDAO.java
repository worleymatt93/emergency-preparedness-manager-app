package com.emergencypreparedness.manager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.emergencypreparedness.manager.entities.Kit;
import java.util.List;

/**
 * Room DAO for {@link Kit} entities.
 * <p>
 * Provides CRUD operations, search, and notification-related queries for top-level kits.
 */
@Dao
public interface KitDAO {

  //region CRUD Operations

  /**
   * Inserts a new kit. Ignores duplicates (if any conflict on unique constraints).
   *
   * @param kit kit to insert
   * @return new row ID, or -1 if ignored due to conflict
   */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(Kit kit);

  /**
   * Updates an existing kit.
   *
   * @param kit kit with updated values
   */
  @Update
  void update(Kit kit);

  /**
   * Deletes a kit.
   *
   * @param kit kit to delete
   */
  @Delete
  void delete(Kit kit);
  //endregion

  //region Queries

  /**
   * Retrieves all kits ordered by ID ascending.
   *
   * @return list of all kits
   */
  @Query("SELECT * FROM Kits ORDER BY kitID ASC")
  List<Kit> getAllKits();

  /**
   * Retrieves a single kit by its ID.
   *
   * @param id kit ID to find
   * @return the matching kit, or null if not found
   */
  @Query("SELECT * FROM Kits WHERE kitID = :id LIMIT 1")
  Kit getKitByID(int id);

  /**
   * Searches kits by name or location (partial, case-insensitive match).
   *
   * @param query search text (e.g., user input)
   * @return matching kits, ordered alphabetically by name
   */
  @Query("SELECT * FROM Kits " +
      "WHERE kitName LIKE '%' || :query || '%' " +
      "OR location LIKE '%' || :query || '%' " +
      "ORDER BY kitName COLLATE NOCASE ASC")
  List<Kit> searchKits(String query);

  /**
   * Retrieves all kits with notifications enabled.
   *
   * @return list of kits where notificationsEnabled = true
   */
  @Query("SELECT * FROM Kits WHERE notificationsEnabled = 1 ORDER BY kitID ASC")
  List<Kit> getKitsWithNotificationsEnabled();

  /**
   * Retrieves kits with notifications enabled and matching a specific frequency.
   *
   * @param frequency notification frequency ("MONTHLY", "QUARTERLY", "YEARLY")
   * @return matching kits
   */
  @Query("SELECT * FROM Kits WHERE notificationsEnabled = 1 " +
      "AND notificationFrequency = :frequency ORDER BY kitID ASC")
  List<Kit> getKitsByFrequency(String frequency);
  //endregion
}
