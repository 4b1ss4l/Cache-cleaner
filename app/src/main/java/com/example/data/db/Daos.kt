package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanHistoryDao {
    @Query("SELECT * FROM clean_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CleanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CleanHistoryEntity): Long

    @Query("DELETE FROM clean_history")
    suspend fun clearAll()

    @Query("DELETE FROM clean_history WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("SELECT SUM(totalBytesCleaned) FROM clean_history")
    fun getTotalFreedBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM clean_history")
    fun getSessionCount(): Flow<Int>
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScan(): ScanHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(item: ScanHistoryEntity): Long
}

@Dao
interface ExcludedAppDao {
    @Query("SELECT * FROM excluded_apps ORDER BY appName ASC")
    fun getAllExcluded(): Flow<List<ExcludedAppEntity>>

    @Query("SELECT packageName FROM excluded_apps")
    suspend fun getExcludedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExcluded(app: ExcludedAppEntity)

    @Delete
    suspend fun removeExcluded(app: ExcludedAppEntity)

    @Query("DELETE FROM excluded_apps WHERE packageName = :packageName")
    suspend fun removeByPackage(packageName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM excluded_apps WHERE packageName = :packageName)")
    suspend fun isExcluded(packageName: String): Boolean
}
