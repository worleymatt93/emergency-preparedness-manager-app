package com.emergencypreparedness.manager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.emergencypreparedness.manager.entities.Category;
import java.util.List;

/**
 * Room DAO for {@link Category} entities.
 * <p>
 * Provides CRUD operations and count query for categories used in kit item dropdowns.
 */
@Dao
public interface CategoryDAO {

  //region CRUD Operations

  /**
   * Inserts a new category. Ignores duplicates (unique index on categoryName).
   *
   * @param category category to insert
   * @return new row ID, or -1 if ignored due to conflict
   */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  long insert(Category category);

  /**
   * Updates an existing category.
   *
   * @param category category to update
   */
  @Update
  void update(Category category);

  /**
   * Deletes a category.
   *
   * @param category category to delete
   */
  @Delete
  void delete(Category category);
  //endregion

  //region Queries

  /**
   * Retrieves all categories sorted alphabetically by name.
   *
   * @return list of all categories
   */
  @Query("SELECT * FROM Categories ORDER BY categoryName ASC")
  List<Category> getAllCategories();

  /**
   * Counts the total number of categories in the table.
   *
   * @return current category count
   */
  @Query("SELECT COUNT(*) FROM Categories")
  int countCategories();
  //endregion
}
