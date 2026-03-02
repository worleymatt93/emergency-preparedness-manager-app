package com.example.emergencypreparednessmanager.database;

import androidx.room.ColumnInfo;

/**
 * Denormalized projection (DTO) used for global item search and inventory report screens.
 * <p>
 * Not a Room entity — populated by JOIN queries in DAOs (e.g., KitItemDAO). Fields map directly to
 * selected columns for display efficiency.
 */
public class ItemSearchRow {

  //region Fields
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

  @ColumnInfo(name = "location")
  public String location;
  //endregion

  //region Getters
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

  public String getLocation() {
    return location;
  }
  //endregion
}
