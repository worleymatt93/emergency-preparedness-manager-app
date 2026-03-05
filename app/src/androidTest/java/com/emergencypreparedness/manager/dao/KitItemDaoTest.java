package com.emergencypreparedness.manager.dao;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.emergencypreparedness.manager.database.DatabaseBuilder;
import com.emergencypreparedness.manager.entities.Category;
import com.emergencypreparedness.manager.entities.Kit;
import com.emergencypreparedness.manager.entities.KitItem;
import com.emergencypreparedness.manager.testutil.TestDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KitItemDaoTest {

  //region Database Setup
  private DatabaseBuilder db;
  private KitDAO kitDao;
  private CategoryDAO categoryDao;
  private KitItemDAO itemDao;

  @Before
  public void setUp() {
    db = TestDatabase.createInMemoryDb();
    kitDao = db.kitDAO();
    categoryDao = db.categoryDAO();
    itemDao = db.kitItemDAO();
  }

  @After
  public void tearDown() {
    db.close();
  }
  //endregion

  //region Insert Item Test
  @Test
  public void insertItem_thenGetById_matchesFields() {
    long kitId = kitDao.insert(new Kit(
        "Test Kit",
        "Loc",
        "",
        false,
        null
    ));
    long catId = categoryDao.insert(new Category("Food"));

    KitItem item = new KitItem(
        (int) kitId,
        "Granola Bars",
        5,
        (int) catId,
        "03/10/2026",
        "",
        false,
        0,
        false
    );

    long itemId = itemDao.insert(item);
    assertTrue(itemId != -1);

    KitItem loaded = itemDao.getItemById((int) itemId);
    assertNotNull(loaded);
    assertEquals("Granola Bars", loaded.getItemName());
    assertEquals(5, loaded.getQuantity());
    assertEquals("03/10/2026", loaded.getExpirationDate());
  }
  //endregion

  //region Adjust Quantity Test
  @Test
  public void adjustQuantity_clampsToZero() {
    long kitId = kitDao.insert(new Kit(
        "Clamp Kit",
        "",
        "",
        false,
        null
    ));
    long catId = categoryDao.insert(new Category("Water"));

    long itemId = itemDao.insert(new KitItem(
        (int) kitId,
        "Bottles",
        2,
        (int) catId,
        "",
        "",
        false,
        0,
        false
    ));

    itemDao.adjustQuantity((int) itemId, -999);

    KitItem loaded = itemDao.getItemById((int) itemId);
    assertNotNull(loaded);
    assertEquals(0, loaded.getQuantity());
  }
  //endregion
}
