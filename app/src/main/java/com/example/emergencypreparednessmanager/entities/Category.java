package com.example.emergencypreparednessmanager.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a category for kit items.
 * <p>
 * Categories are seeded with defaults on first run and can be managed by the user. Enforces unique
 * category names via index.
 */
@Entity(
    tableName = "Categories",
    indices = {@Index(value = {"categoryName"}, unique = true)}
)
public class Category {

  //region Fields
  @PrimaryKey(autoGenerate = true)
  private int categoryID;

  private String categoryName;
  //endregion

//region Constructors

  /**
   * Creates a new category with the given name (used when inserting new categories).
   *
   * @param categoryName the display name of the category
   */
  public Category(String categoryName) {
    this.categoryName = categoryName;
  }

  @Ignore
  public Category(int categoryID, String categoryName) {
    this.categoryID = categoryID;
    this.categoryName = categoryName;
  }
  //endRegion

  //region Getters and Setters
  public int getCategoryID() {
    return categoryID;
  }

  public void setCategoryID(int categoryID) {
    this.categoryID = categoryID;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }
  //endregion
}
