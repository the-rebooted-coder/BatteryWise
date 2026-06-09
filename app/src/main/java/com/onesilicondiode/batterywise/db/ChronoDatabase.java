package com.onesilicondiode.batterywise.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChargeSession.class, BatterySnapshot.class}, version = 1, exportSchema = false)
public abstract class ChronoDatabase extends RoomDatabase {

    private static volatile ChronoDatabase INSTANCE;

    public abstract ChronoDao chronoDao();

    public static ChronoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChronoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ChronoDatabase.class,
                            "chrono_cell_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
