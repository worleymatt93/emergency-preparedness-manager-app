package com.example.emergencypreparednessmanager.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.emergencypreparednessmanager.entities.Category;

import java.util.List;

/**
 * DAO for Category entities.
 * Used for scalable dropdown values.
 */
@Dao
public interface CategoryDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM Categories ORDER BY categoryName ASC")
    List<Category> getAllCategories();
}
