package com.example.emergencypreparednessmanager.database;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.emergencypreparednessmanager.dao.CategoryDAO;
import com.example.emergencypreparednessmanager.dao.KitDAO;
import com.example.emergencypreparednessmanager.dao.KitItemDAO;
import com.example.emergencypreparednessmanager.entities.Category;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.entities.KitItem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room database definition and singleton builder for the app.
 * <p>
 * Features:
 * <ul>
 *   <li>Uses {@link RoomDatabase.Builder#fallbackToDestructiveMigration()} during development — wipes all data on schema changes</li>
 *   <li>Seeds default categories on first creation and re-seeds on every open if the table is empty</li>
 * </ul>
 * <strong>Warning</strong>: In production, replace fallbackToDestructiveMigration() with proper migrations.
 */
@Database(
    entities = {Kit.class, KitItem.class, Category.class},
    version = 15, // Do not increase without adding a migration or Room will throw at startup
    exportSchema = true
)
public abstract class DatabaseBuilder extends RoomDatabase {

  //region Constants
  private static final String TAG = "DatabaseBuilder";
  private static final String DB_NAME = "EmergencyPreparedness.db";

  /**
   * Single background thread for one-time tasks like seeding.
   */
  private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
  //endregion

  //region Singleton Instance
  private static volatile DatabaseBuilder INSTANCE;

  /**
   * Returns the singleton database instance using double-checked locking.
   *
   * @param context any context (application context is used internally)
   * @return the singleton Room database instance
   */
  public static DatabaseBuilder getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (DatabaseBuilder.class) {
        if (INSTANCE == null) {
          Log.d(TAG, "Building Database instance");

          INSTANCE = Room.databaseBuilder(
                  context.getApplicationContext(),
                  DatabaseBuilder.class,
                  DB_NAME
              )
              // No destructive migration.
              // When v16 is released, add:
              // .addMigrations(MIGRATION_15_16)
              .addCallback(new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                  super.onCreate(db);
                  Log.d(TAG, "Database created - seeding defaults");
                  databaseExecutor.execute(() -> ensureDefaultCategories(INSTANCE));
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                  super.onOpen(db);
                  // Re-seed if empty
                  databaseExecutor.execute(() -> ensureDefaultCategories(INSTANCE));
                }

                @Override
                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                  super.onDestructiveMigration(db);
                  Log.d(TAG, "Destructive migration occurred - all data wiped.");
                }
              })
              .build();
        }
      }
    }
    return INSTANCE;
  }
  //endregion

  //region Seeding

  /**
   * Ensures default categories exist. Inserts the standard set only if the table is empty.
   */
  private static void ensureDefaultCategories(DatabaseBuilder db) {
    if (db == null) {
      Log.e(TAG, "ensureDefaultCategories called with null db");
      return;
    }

    try {
      CategoryDAO dao = db.categoryDAO();
      int count = dao.countCategories();

      if (count > 0) {
        Log.d(TAG, "Categories already exist (" + count + "). Skipping seed.");
        return;
      }

      db.runInTransaction(() -> {
        dao.insert(new Category("Water"));
        dao.insert(new Category("Food"));
        dao.insert(new Category("Medical"));
        dao.insert(new Category("Tools"));
        dao.insert(new Category("Power"));
        dao.insert(new Category("Documents"));
        dao.insert(new Category("Clothing"));
        dao.insert(new Category("Hygiene"));
      });

      Log.d(TAG, "Default categories SEEDED");
    } catch (Exception e) {
      Log.e(TAG, "Error seeding categories", e);
    }
  }
  //endregion

  //region DAOs
  public abstract KitDAO kitDAO();

  public abstract KitItemDAO kitItemDAO();

  public abstract CategoryDAO categoryDAO();
  //endregion
}
