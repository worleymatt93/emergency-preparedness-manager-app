package com.example.emergencypreparednessmanager.testutil;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import com.example.emergencypreparednessmanager.database.DatabaseBuilder;

public class TestDatabase {

  private TestDatabase() {}

  public static DatabaseBuilder createInMemoryDb() {
    Context context = ApplicationProvider.getApplicationContext();

    return Room.inMemoryDatabaseBuilder(context, DatabaseBuilder.class)
        .allowMainThreadQueries()
        .build();
  }

}
