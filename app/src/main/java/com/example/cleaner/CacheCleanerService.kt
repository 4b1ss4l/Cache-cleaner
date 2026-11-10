package com.example.cleaner

import android.content.Context
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.CleanHistoryEntity
import com.example.model.AppCacheInfo
import com.example.model.AppCleanDetail
import com.example.model.CleanOutcome
import com.example.model.CleanReport
import com.example.model.CleanType
import com.example.model.CleaningProgress
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CacheCleanerService(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {

    companion object {
        const val TAG = "CacheCleanerService"
    }

    private val db = AppDatabase.getInstance(context)

    fun executeClean(
        targetApps: List<AppCacheInfo>,
        cleanType: CleanType
    ): Flow<Pair<CleaningProgress, CleanReport?>> = flow {
        val totalCount = targetApps.size
        var cleanedCount = 0
        var skippedCount = 0
        var failedCount = 0
        var bytesCleanedSoFar = 0L

        val details = ArrayList<AppCleanDetail>(totalCount)
        val isPrivileged = shizukuManager.status.value.isAvailable

        emit(
            Pair(
                CleaningProgress(
                    isRunning = true,
                    currentApp = "Iniciando limpeza...",
                    currentIndex = 0,
                    totalCount = totalCount,
                    cleanedCount = 0,
                    skippedCount = 0,
                    failedCount = 0,
                    bytesCleanedSoFar = 0L
                ),
                null
            )
        )

        // If privileged and cleaning all/selective with substantial apps, execute system trim-caches
        if (isPrivileged && (cleanType == CleanType.ALL || cleanType == CleanType.SELECTIVE)) {
            val totalTargetCache = targetApps.sumOf { it.cacheBytes }
            if (totalTargetCache > 0L) {
                shizukuManager.trimCaches(999999999999L)
            }
        }

        for ((index, app) in targetApps.withIndex()) {
            emit(
                Pair(
                    CleaningProgress(
                        isRunning = true,
                        currentApp = app.appName,
                        currentIndex = index + 1,
                        totalCount = totalCount,
                        cleanedCount = cleanedCount,
                        skippedCount = skippedCount,
                        failedCount = failedCount,
                        bytesCleanedSoFar = bytesCleanedSoFar
                    ),
                    null
                )
            )

            val detail = cleanApp(app, isPrivileged)
            details.add(detail)

            when (detail.outcome) {
                CleanOutcome.SUCCESS -> {
                    cleanedCount++
                    bytesCleanedSoFar += detail.cacheBytesCleaned
                }
                CleanOutcome.NO_CACHE -> {
                    skippedCount++
                }
                CleanOutcome.PERMISSION_DENIED -> {
                    failedCount++
                }
                CleanOutcome.ERROR -> {
                    failedCount++
                }
            }

            emit(
                Pair(
                    CleaningProgress(
                        isRunning = true,
                        currentApp = app.appName,
                        currentIndex = index + 1,
                        totalCount = totalCount,
                        cleanedCount = cleanedCount,
                        skippedCount = skippedCount,
                        failedCount = failedCount,
                        bytesCleanedSoFar = bytesCleanedSoFar
                    ),
                    null
                )
            )
        }

        val finalReport = CleanReport(
            timestamp = System.currentTimeMillis(),
            type = cleanType,
            appsProcessed = totalCount,
            appsCleaned = cleanedCount,
            appsSkipped = skippedCount,
            appsFailed = failedCount,
            totalBytesCleaned = bytesCleanedSoFar,
            details = details,
            wasPrivileged = isPrivileged
        )

        // Persist history to Room
        saveHistoryToDb(finalReport)

        emit(
            Pair(
                CleaningProgress(
                    isRunning = false,
                    currentApp = "Concluído",
                    currentIndex = totalCount,
                    totalCount = totalCount,
                    cleanedCount = cleanedCount,
                    skippedCount = skippedCount,
                    failedCount = failedCount,
                    bytesCleanedSoFar = bytesCleanedSoFar
                ),
                finalReport
            )
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun cleanApp(app: AppCacheInfo, isPrivileged: Boolean): AppCleanDetail {
        if (app.isProtected) {
            return AppCleanDetail(
                packageName = app.packageName,
                appName = app.appName,
                cacheBytesBefore = app.cacheBytes,
                cacheBytesCleaned = 0L,
                outcome = CleanOutcome.NO_CACHE,
                message = "Aplicativo na lista de proteção (ignorado)"
            )
        }

        if (app.cacheBytes <= 0L) {
            return AppCleanDetail(
                packageName = app.packageName,
                appName = app.appName,
                cacheBytesBefore = 0L,
                cacheBytesCleaned = 0L,
                outcome = CleanOutcome.NO_CACHE,
                message = "Sem cache disponível para limpeza"
            )
        }

        // Own app cache can always be completely wiped
        if (app.packageName == context.packageName) {
            var freed = 0L
            try {
                freed += wipeDirectory(context.cacheDir)
                context.externalCacheDir?.let { freed += wipeDirectory(it) }
                return AppCleanDetail(
                    packageName = app.packageName,
                    appName = app.appName,
                    cacheBytesBefore = app.cacheBytes,
                    cacheBytesCleaned = freed.coerceAtLeast(app.cacheBytes),
                    outcome = CleanOutcome.SUCCESS,
                    message = "Cache interno limpo com sucesso"
                )
            } catch (e: Exception) {
                return AppCleanDetail(
                    packageName = app.packageName,
                    appName = app.appName,
                    cacheBytesBefore = app.cacheBytes,
                    cacheBytesCleaned = freed,
                    outcome = CleanOutcome.ERROR,
                    message = "Falha ao limpar cache interno: ${e.message}"
                )
            }
        }

        if (isPrivileged) {
            try {
                // Remove public external cache
                shizukuManager.clearPackageExternalCache(app.packageName)
                // Trim caches
                val trimResult = shizukuManager.trimCaches(app.cacheBytes.coerceAtLeast(1024L * 1024L))

                return if (trimResult.success) {
                    AppCleanDetail(
                        packageName = app.packageName,
                        appName = app.appName,
                        cacheBytesBefore = app.cacheBytes,
                        cacheBytesCleaned = app.cacheBytes,
                        outcome = CleanOutcome.SUCCESS,
                        message = "Limpeza de cache concluída via Shizuku"
                    )
                } else {
                    AppCleanDetail(
                        packageName = app.packageName,
                        appName = app.appName,
                        cacheBytesBefore = app.cacheBytes,
                        cacheBytesCleaned = 0L,
                        outcome = CleanOutcome.ERROR,
                        message = "Falha na operação: ${trimResult.error}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning app with Shizuku: ${app.packageName}", e)
                return AppCleanDetail(
                    packageName = app.packageName,
                    appName = app.appName,
                    cacheBytesBefore = app.cacheBytes,
                    cacheBytesCleaned = 0L,
                    outcome = CleanOutcome.ERROR,
                    message = e.message ?: "Erro desconhecido"
                )
            }
        } else {
            // Unprivileged mode: attempt standard public cache cleanup if writable
            var cleanedBytes = 0L
            val publicDir = File("/sdcard/Android/data/${app.packageName}/cache")
            if (publicDir.exists() && publicDir.canWrite()) {
                cleanedBytes = wipeDirectory(publicDir)
            }

            return if (cleanedBytes > 0L) {
                AppCleanDetail(
                    packageName = app.packageName,
                    appName = app.appName,
                    cacheBytesBefore = app.cacheBytes,
                    cacheBytesCleaned = cleanedBytes,
                    outcome = CleanOutcome.SUCCESS,
                    message = "Cache público removido"
                )
            } else {
                AppCleanDetail(
                    packageName = app.packageName,
                    appName = app.appName,
                    cacheBytesBefore = app.cacheBytes,
                    cacheBytesCleaned = 0L,
                    outcome = CleanOutcome.PERMISSION_DENIED,
                    message = "Operação não permitida sem Shizuku. Use as configurações do sistema."
                )
            }
        }
    }

    private fun wipeDirectory(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            freed += if (child.isDirectory) {
                wipeDirectory(child)
            } else {
                val len = child.length()
                if (child.delete()) len else 0L
            }
        }
        return freed
    }

    private suspend fun saveHistoryToDb(report: CleanReport) = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            report.details.forEach { detail ->
                val obj = JSONObject().apply {
                    put("packageName", detail.packageName)
                    put("appName", detail.appName)
                    put("cacheBytesBefore", detail.cacheBytesBefore)
                    put("cacheBytesCleaned", detail.cacheBytesCleaned)
                    put("outcome", detail.outcome.name)
                    put("message", detail.message)
                }
                jsonArray.put(obj)
            }

            val entity = CleanHistoryEntity(
                timestamp = report.timestamp,
                cleanType = report.type.name,
                totalBytesCleaned = report.totalBytesCleaned,
                appsProcessed = report.appsProcessed,
                appsCleaned = report.appsCleaned,
                appsSkipped = report.appsSkipped,
                appsFailed = report.appsFailed,
                wasPrivileged = report.wasPrivileged,
                detailsJson = jsonArray.toString()
            )
            db.cleanHistoryDao().insertHistory(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save clean history", e)
        }
    }
}
