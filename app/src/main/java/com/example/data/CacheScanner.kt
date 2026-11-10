package com.example.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.model.AppCacheInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ScanProgressEvent {
    data class Started(val totalApps: Int) : ScanProgressEvent()
    data class AppScanned(
        val app: AppCacheInfo,
        val currentIndex: Int,
        val totalApps: Int,
        val currentTotalCache: Long
    ) : ScanProgressEvent()
    data class Completed(
        val apps: List<AppCacheInfo>,
        val totalCacheBytes: Long,
        val userAppsCacheBytes: Long,
        val systemAppsCacheBytes: Long
    ) : ScanProgressEvent()
    data class Failed(val error: String) : ScanProgressEvent()
}

class CacheScanner(private val context: Context) {

    companion object {
        const val TAG = "CacheScanner"
    }

    private val db = AppDatabase.getInstance(context)

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
    }

    fun scanAppsFlow(): Flow<ScanProgressEvent> = flow {
        val pm = context.packageManager
        val installedApps = try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emit(ScanProgressEvent.Failed("Erro ao obter lista de aplicativos: ${e.message}"))
            return@flow
        }

        val totalApps = installedApps.size
        emit(ScanProgressEvent.Started(totalApps))

        val hasUsageStats = hasUsageStatsPermission()
        val storageStatsManager = if (hasUsageStats && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else null

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val userHandle = Process.myUserHandle()
        val excludedPackageNames = db.excludedAppDao().getExcludedPackageNames().toSet()

        val results = ArrayList<AppCacheInfo>(totalApps)
        var runningTotalCache = 0L

        installedApps.forEachIndexed { index, appInfo ->
            val packageName = appInfo.packageName
            val appName = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                packageName
            }

            val versionName = try {
                val packageInfo = pm.getPackageInfo(packageName, 0)
                packageInfo.versionName ?: "1.0"
            } catch (_: Exception) {
                "1.0"
            }

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            var cacheBytes = 0L
            var codeBytes = 0L
            var dataBytes = 0L

            if (storageStatsManager != null && storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val volumes = storageManager.storageVolumes
                    for (volume in volumes) {
                        val uuid = if (volume.isPrimary) {
                            StorageManager.UUID_DEFAULT
                        } else {
                            volume.uuid?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        }
                        if (uuid != null) {
                            try {
                                val stats = storageStatsManager.queryStatsForPackage(uuid, packageName, userHandle)
                                cacheBytes += stats.cacheBytes
                                codeBytes += stats.appBytes
                                dataBytes += stats.dataBytes
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Storage stats unavailable for $packageName: ${e.message}")
                }
            } else {
                // Fallback check on external cache directory
                cacheBytes = getExternalCacheSize(packageName)
            }

            // Own app cache can always be accurately measured
            if (packageName == context.packageName) {
                val ownInternalCache = getFolderSize(context.cacheDir)
                val ownExternalCache = context.externalCacheDir?.let { getFolderSize(it) } ?: 0L
                cacheBytes = (cacheBytes).coerceAtLeast(ownInternalCache + ownExternalCache)
            }

            val isProtected = excludedPackageNames.contains(packageName)

            val appCacheInfo = AppCacheInfo(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                isSystemApp = isSystem,
                cacheBytes = cacheBytes,
                codeBytes = codeBytes,
                dataBytes = dataBytes,
                isProtected = isProtected,
                uid = appInfo.uid,
                targetSdkVersion = appInfo.targetSdkVersion,
                dataDir = appInfo.dataDir ?: ""
            )

            results.add(appCacheInfo)
            runningTotalCache += cacheBytes

            emit(
                ScanProgressEvent.AppScanned(
                    app = appCacheInfo,
                    currentIndex = index + 1,
                    totalApps = totalApps,
                    currentTotalCache = runningTotalCache
                )
            )
        }

        // Sort descending by cache size
        results.sortByDescending { it.cacheBytes }

        val totalCache = results.sumOf { it.cacheBytes }
        val userCache = results.filter { !it.isSystemApp }.sumOf { it.cacheBytes }
        val systemCache = results.filter { it.isSystemApp }.sumOf { it.cacheBytes }

        emit(
            ScanProgressEvent.Completed(
                apps = results,
                totalCacheBytes = totalCache,
                userAppsCacheBytes = userCache,
                systemAppsCacheBytes = systemCache
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun getExternalCacheSize(packageName: String): Long {
        return try {
            val dir = File("/sdcard/Android/data/$packageName/cache")
            if (dir.exists() && dir.canRead()) {
                getFolderSize(dir)
            } else 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (!file.isDirectory) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (child in children) {
            size += if (child.isDirectory) getFolderSize(child) else child.length()
        }
        return size
    }
}
