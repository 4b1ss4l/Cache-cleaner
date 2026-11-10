package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cleaner.CacheCleanerService
import com.example.data.CacheScanner
import com.example.data.SettingsRepository
import com.example.model.AutoCleanTarget
import com.example.model.CleanType
import com.example.data.ScanProgressEvent
import com.example.shizuku.ShizukuManager
import com.example.util.FormatUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull

class AutoCleanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepo = SettingsRepository(appContext)
        val settings = settingsRepo.settingsFlow.firstOrNull() ?: return Result.success()

        val shizukuManager = ShizukuManager(appContext)
        val isPrivileged = shizukuManager.status.value.isAvailable

        if (settings.autoCleanRequireConfirmation) {
            NotificationHelper.notifyAutoCleanResult(
                appContext,
                "Limpeza automática agendada pronta para execução. Toque para confirmar."
            )
            settingsRepo.recordAutoCleanResult(
                System.currentTimeMillis(),
                "Aguardando confirmação do usuário (notificação enviada)"
            )
            return Result.success()
        }

        if (!isPrivileged) {
            val message = "Limpeza em segundo plano requer Shizuku ativo. Toque para gerenciar o cache."
            if (settings.notificationsEnabled) {
                NotificationHelper.notifyAutoCleanResult(appContext, message)
            }
            settingsRepo.recordAutoCleanResult(
                System.currentTimeMillis(),
                "Falha: Shizuku indisponível para execução em segundo plano"
            )
            return Result.success()
        }

        // Privileged scan and clean
        val scanner = CacheScanner(appContext)
        val scanResult = scanner.scanAppsFlow().lastOrNull()

        if (scanResult !is ScanProgressEvent.Completed) {
            settingsRepo.recordAutoCleanResult(System.currentTimeMillis(), "Falha na varredura")
            return Result.failure()
        }

        val targetApps = when (settings.autoCleanTarget) {
            AutoCleanTarget.USER_ONLY -> scanResult.apps.filter { !it.isSystemApp && !it.isProtected && it.hasCache }
            AutoCleanTarget.SYSTEM_ONLY -> scanResult.apps.filter { it.isSystemApp && !it.isProtected && it.hasCache }
            AutoCleanTarget.BOTH -> scanResult.apps.filter { !it.isProtected && it.hasCache }
        }

        if (targetApps.isEmpty()) {
            settingsRepo.recordAutoCleanResult(System.currentTimeMillis(), "Nenhum aplicativo com cache elegível encontrado")
            return Result.success()
        }

        val cleaner = CacheCleanerService(appContext, shizukuManager)
        val cleanPair = cleaner.executeClean(targetApps, CleanType.AUTO).lastOrNull()
        val report = cleanPair?.second

        if (report != null) {
            val freedStr = FormatUtils.formatBytes(report.totalBytesCleaned)
            val summary = "Limpeza concluída: $freedStr liberados em ${report.appsCleaned} apps."
            settingsRepo.recordAutoCleanResult(System.currentTimeMillis(), summary)

            if (settings.notificationsEnabled && settings.notifyOnCleanComplete) {
                NotificationHelper.notifyCleanComplete(appContext, report.totalBytesCleaned, report.appsCleaned)
            }
        }

        return Result.success()
    }
}
