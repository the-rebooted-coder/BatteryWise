package com.onesilicondiode.batterywise.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "battery_snapshots")
public class BatterySnapshot {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestampMs;

    public int batteryPercent;

    /** Temperature in °C (e.g. 31.2) */
    public float temperature;

    /** Voltage in volts (e.g. 4.2) */
    public float voltage;

    public boolean isCharging;

    /**
     * Health status matching BatteryManager.BATTERY_HEALTH_* constants:
     * 2 = GOOD, 3 = OVERHEAT, 4 = DEAD, 5 = OVER_VOLTAGE, 6 = FAILURE, 7 = COLD
     */
    public int healthStatus;
}
