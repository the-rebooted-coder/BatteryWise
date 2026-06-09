package com.onesilicondiode.batterywise.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "charge_sessions")
public class ChargeSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long startTimeMs;
    public long endTimeMs;

    public int startPercent;
    public int endPercent;

    /** Average temperature during the session in °C (e.g. 32.5) */
    public float avgTemperature;

    /** Peak temperature during the session in °C */
    public float maxTemperature;

    /** Average wattage during the session */
    public float avgWattage;

    /**
     * Charger type matching BatteryManager.BATTERY_PLUGGED_* constants:
     * 1 = AC, 2 = USB, 4 = Wireless, 0 = Unknown/Discharge
     */
    public int chargerType;

    /** Whether SafeCharge alert was triggered during this session */
    public boolean safeChargeTriggered;
}
