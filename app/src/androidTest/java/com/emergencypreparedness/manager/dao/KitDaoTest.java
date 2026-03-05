package com.emergencypreparedness.manager.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.emergencypreparedness.manager.database.DatabaseBuilder;
import com.emergencypreparedness.manager.entities.Kit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class KitDaoTest {

  //region Database Setup
  private DatabaseBuilder db;
  private KitDAO kitDao;

  @Before
  public void createDb() {
    Context context = ApplicationProvider.getApplicationContext();

    db = Room.inMemoryDatabaseBuilder(context, DatabaseBuilder.class)
        .allowMainThreadQueries()
        .build();

    kitDao = db.kitDAO();
  }

  @After
  public void closeDb() {
    db.close();
  }
  //endregion

  //region Insert Kit Test
  /**
   * Verifies that inserting a kit and retrieving it by ID
   * returns the same stored values.
   */
  @Test
  public void insertKit_thenGetById_matchesFields() {

    Kit kit = new Kit();
    kit.setKitName("Car Kit");
    kit.setLocation("Trunk");
    kit.setNotes("Emergency supplies");
    kit.setNotificationsEnabled(false);

    long id = kitDao.insert(kit);

    Kit loaded = kitDao.getKitByID((int) id);

    assertNotNull(loaded);
    assertEquals("Car Kit", loaded.getKitName());
    assertEquals("Trunk", loaded.getLocation());
    assertEquals("Emergency supplies", loaded.getNotes());
  }
  //endregion

  //region Search Kits Test
  /**
   * Verifies that the search query finds kits
   * by name or location.
   */
  @Test
  public void searchKits_findsByNameOrLocation() {

    Kit kit1 = new Kit();
    kit1.setKitName("Car Kit");
    kit1.setLocation("Trunk");

    Kit kit2 = new Kit();
    kit2.setKitName("Home Supplies");
    kit2.setLocation("Garage");

    kitDao.insert(kit1);
    kitDao.insert(kit2);

    List<Kit> results = kitDao.searchKits("Car");

    assertEquals(1, results.size());
    assertEquals("Car Kit", results.get(0).getKitName());
  }
  //endregion

  //region Update Kit Test
  /**
   * Verifies that updating a kit persists
   * the changed values in the database.
   */
  @Test
  public void updateKit_persistsChanges() {

    Kit kit = new Kit();
    kit.setKitName("Old Name");
    kit.setLocation("Closet");

    long id = kitDao.insert(kit);

    Kit loaded = kitDao.getKitByID((int) id);
    loaded.setKitName("Updated Name");

    kitDao.update(loaded);

    Kit updated = kitDao.getKitByID((int) id);

    assertEquals("Updated Name", updated.getKitName());
  }
  //endregion
}