package com.emergencypreparedness.manager.dao;

import static org.junit.Assert.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.emergencypreparedness.manager.database.DatabaseBuilder;
import com.emergencypreparedness.manager.entities.Category;
import com.emergencypreparedness.manager.testutil.TestDatabase;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CategoryDaoTest {

  //region Database Setup
  private DatabaseBuilder db;
  private CategoryDAO categoryDao;

  @Before
  public void setUp() {
    db = TestDatabase.createInMemoryDb();
    categoryDao = db.categoryDAO();
  }

  @After
  public void tearDown() {
    db.close();
  }
  //endregion

  //region Insert Category Test
  @Test
  public void insertCategory_thenGetAll_containsIt() {
    long id = categoryDao.insert(new Category("Water"));
    assertTrue(id != -1);

    List<Category> all = categoryDao.getAllCategories();
    assertFalse(all.isEmpty());

    boolean found = false;
    for (Category c : all) {
      if ("Water".equals(c.getCategoryName())) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }
  //endregion

  //region Duplicate Category Test
  @Test
  public void duplicateCategoryName_isIgnored_returnsMinusOne() {
    long first = categoryDao.insert(new Category("Food"));
    long second = categoryDao.insert(new Category("Food"));

    assertTrue(first != -1)
    ;
    assertEquals(-1L, second);
  }
  //endregion
}
