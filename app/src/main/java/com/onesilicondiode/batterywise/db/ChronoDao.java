package com.onesilicondiode.batterywise.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChronoDao {

    // ── Charge Sessions ──

    @Insert
    void insertSession(ChargeSession session);

    @Query("SELECT * FROM charge_sessions WHERE startTimeMs >= :sinceMs ORDER BY startTimeMs ASC")
    List<ChargeSession> getSessionsSince(long sinceMs);

    @Query("SELECT * FROM charge_sessions ORDER BY startTimeMs ASC")
    List<ChargeSession> getAllSessions();

    @Query("SELECT COUNT(*) FROM charge_sessions")
    int getSessionCount();

    @Query("SELECT AVG(avgTemperature) FROM charge_sessions WHERE avgTemperature > 0")
    float getAverageSessionTemperature();

    @Query("SELECT SUM(endTimeMs - startTimeMs) FROM charge_sessions")
    long getTotalChargingDurationMs();

    // ── Battery Snapshots ──

    @Insert
    void insertSnapshot(BatterySnapshot snapshot);

    @Query("SELECT * FROM battery_snapshots WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    List<BatterySnapshot> getSnapshotsSince(long sinceMs);

    @Query("SELECT * FROM battery_snapshots ORDER BY timestampMs DESC LIMIT :limit")
    List<BatterySnapshot> getRecentSnapshots(int limit);

    @Query("SELECT * FROM battery_snapshots ORDER BY timestampMs ASC")
    List<BatterySnapshot> getAllSnapshots();

    // ── Housekeeping ──

    @Query("DELETE FROM charge_sessions WHERE startTimeMs < :cutoffMs")
    void deleteOldSessions(long cutoffMs);

    @Query("DELETE FROM battery_snapshots WHERE timestampMs < :cutoffMs")
    void deleteOldSnapshots(long cutoffMs);
}
