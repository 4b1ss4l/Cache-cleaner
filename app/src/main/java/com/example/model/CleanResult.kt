package com.example.model

enum class CleanOutcome {
    SUCCESS,
    NO_CACHE,
    PERMISSION_DENIED,
    ERROR
}

data class AppCleanDetail(
    val packageName: String,
    val appName: String,
    val cacheBytesBefore: Long,
    val cacheBytesCleaned: Long,
    val outcome: CleanOutcome,
    val message: String = ""
)

enum class CleanType {
    ALL,
    SELECTIVE,
    INDIVIDUAL,
    AUTO
}

data class CleanReport(
    val timestamp: Long = System.currentTimeMillis(),
    val type: CleanType,
    val appsProcessed: Int,
    val appsCleaned: Int,
    val appsSkipped: Int,
    val appsFailed: Int,
    val totalBytesCleaned: Long,
    val details: List<AppCleanDetail> = emptyList(),
    val wasPrivileged: Boolean = false
)

data class CleaningProgress(
    val isRunning: Boolean = false,
    val currentApp: String = "",
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val cleanedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val bytesCleanedSoFar: Long = 0L
)
