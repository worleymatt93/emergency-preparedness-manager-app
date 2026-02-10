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

@Database(entities = {Kit.class, KitItem.class, Category.class}, version = 1, exportSchema = false)
public abstract class DatabaseBuilder extends RoomDatabase {

    private static final String TAG = "DatabaseBuilder";
    private static volatile DatabaseBuilder INSTANCE;
    private static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // ------------------- DAOs -------------------

    public abstract KitDAO kitDAO();
    public abstract KitItemDAO kitItemDAO();
    public abstract CategoryDAO categoryDAO();

    // ------------------- SINGLETON GETTER -------------------

    static DatabaseBuilder getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseBuilder.class) {
                if (INSTANCE == null) {

                    Log.d(TAG, "Building Database instance");

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    DatabaseBuilder.class,
                                    "EmergencyPreparedness.db"
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d(TAG, "Database CREATED");

                                    // Seed default categories on first creation
                                    databaseExecutor.execute(() -> seedDefaultCategories(context));
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    Log.d(TAG, "Database DESTRUCTIVE MIGRATION occurred. All data wiped.");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void seedDefaultCategories(Context context) {
        try {
            DatabaseBuilder db = getDatabase(context);
            CategoryDAO dao = db.categoryDAO();

            dao.insert(new Category("Water"));
            dao.insert(new Category("Food"));
            dao.insert(new Category("Medical"));
            dao.insert(new Category("Tools"));
            dao.insert(new Category("Power"));
            dao.insert(new Category("Documents"));
            dao.insert(new Category("Clothing"));
            dao.insert(new Category("Hygiene"));

            Log.d(TAG, "Default categories SEEDED");
        } catch (Exception e) {
            Log.e(TAG, "Error seeding categories", e);
        }
    }
}

