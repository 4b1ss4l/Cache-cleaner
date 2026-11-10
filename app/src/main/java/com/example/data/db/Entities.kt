package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clean_history")
data class CleanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val cleanType: String,
    val totalBytesCleaned: Long,
    val appsProcessed: Int,
    val appsCleaned: Int,
    val appsSkipped: Int,
    val appsFailed: Int,
    val wasPrivileged: Boolean,
    val detailsJson: String
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val totalCacheBytes: Long,
    val userAppsCacheBytes: Long,
    val systemAppsCacheBytes: Long,
    val appsScanned: Int,
    val appsWithCache: Int
)

@Entity(tableName = "excluded_apps")
data class ExcludedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
