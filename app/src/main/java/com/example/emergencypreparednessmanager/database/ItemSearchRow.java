package com.example.emergencypreparednessmanager.database;

import androidx.room.ColumnInfo;

/**
 * Row model for global item search results (KitItems JOIN Kits JOIN Categories).
 */
public class ItemSearchRow {

    @ColumnInfo(name = "itemID")
    public int itemID;

    @ColumnInfo(name = "kitID")
    public int kitID;

    @ColumnInfo(name = "itemName")
    public String itemName;

    @ColumnInfo(name = "quantity")
    public int quantity;

    @ColumnInfo(name = "expirationDate")
    public String expirationDate;

    @ColumnInfo(name = "kitName")
    public String kitName;

    @ColumnInfo(name = "categoryName")
    public String categoryName;

    // ---- Getters ----

    public int getItemID() {
        return itemID;
    }

    public int getKitID() {
        return kitID;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public String getKitName() {
        return kitName;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
