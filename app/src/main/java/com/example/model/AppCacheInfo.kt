package com.example.model

data class AppCacheInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val cacheBytes: Long,
    val codeBytes: Long = 0L,
    val dataBytes: Long = 0L,
    val isProtected: Boolean = false,
    val lastCleanedTimestamp: Long? = null,
    val uid: Int = 0,
    val targetSdkVersion: Int = 0,
    val dataDir: String = ""
) {
    val hasCache: Boolean
        get() = cacheBytes > 0L

    val isEligibleForClean: Boolean
        get() = !isProtected && hasCache
}
