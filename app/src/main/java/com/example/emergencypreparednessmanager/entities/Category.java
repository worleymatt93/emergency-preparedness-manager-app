package com.example.emergencypreparednessmanager.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a category for kit items.
 * Categories are seeded on first run and may be user-managed.
 */
@Entity(tableName = "Categories")
public class Category {

    // ------------------- DATABASE FIELDS -------------------

    @PrimaryKey(autoGenerate = true)
    private int categoryID;
    private String categoryName;

    // ------------------- CONSTRUCTORS -------------------

    public Category(String categoryName) {
        this.categoryName = categoryName;
    }

    @Ignore
    public Category(int categoryID, String categoryName) {
        this.categoryID = categoryID;
        this.categoryName = categoryName;
    }

    // ------------------- GETTERS AND SETTERS -------------------

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
}
