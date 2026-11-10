package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleaner.CacheCleanerService
import com.example.data.CacheScanner
import com.example.data.ScanProgressEvent
import com.example.data.SettingsRepository
import com.example.data.db.AppDatabase
import com.example.data.db.CleanHistoryEntity
import com.example.data.db.ExcludedAppEntity
import com.example.data.db.ScanHistoryEntity
import com.example.model.AppCacheInfo
import com.example.model.AppSettings
import com.example.model.AutoCleanFrequency
import com.example.model.AutoCleanTarget
import com.example.model.CleanReport
import com.example.model.CleanType
import com.example.model.CleaningProgress
import com.example.model.ShizukuStatus
import com.example.model.ThemeMode
import com.example.shizuku.ShizukuManager
import com.example.util.NotificationHelper
import com.example.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    LARGEST_CACHE,
    NAME_ASC,
    NAME_DESC
}

data class ScanUiState(
    val isScanning: Boolean = false,
    val progressIndex: Int = 0,
    val totalCount: Int = 0,
    val currentAppName: String = "",
    val totalCacheFound: Long = 0L,
    val userAppsCache: Long = 0L,
    val systemAppsCache: Long = 0L,
    val allApps: List<AppCacheInfo> = emptyList(),
    val lastScanTimestamp: Long = 0L,
    val hasUsagePermission: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val shizukuManager = ShizukuManager(application)
    val settingsRepo = SettingsRepository(application)
    private val scanner = CacheScanner(application)
    private val cleaner = CacheCleanerService(application, shizukuManager)

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.status

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val cleanHistory: StateFlow<List<CleanHistoryEntity>> = db.cleanHistoryDao().getAllHistory().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalFreedBytes: StateFlow<Long?> = db.cleanHistoryDao().getTotalFreedBytes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )

    val totalSessionsCount: StateFlow<Int> = db.cleanHistoryDao().getSessionCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val excludedApps: StateFlow<List<ExcludedAppEntity>> = db.excludedAppDao().getAllExcluded().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _cleaningProgress = MutableStateFlow(CleaningProgress())
    val cleaningProgress: StateFlow<CleaningProgress> = _cleaningProgress.asStateFlow()

    private val _latestReport = MutableStateFlow<CleanReport?>(null)
    val latestReport: StateFlow<CleanReport?> = _latestReport.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    // Apps screen selection & filter state
    private val _selectedPackageNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackageNames: StateFlow<Set<String>> = _selectedPackageNames.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterOnlyWithCache = MutableStateFlow(false)
    val filterOnlyWithCache: StateFlow<Boolean> = _filterOnlyWithCache.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.LARGEST_CACHE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedAppForDetail = MutableStateFlow<AppCacheInfo?>(null)
    val selectedAppForDetail: StateFlow<AppCacheInfo?> = _selectedAppForDetail.asStateFlow()

    init {
        NotificationHelper.createNotificationChannel(application)
        checkUsagePermission()

        viewModelScope.launch {
            val latestScan = db.scanHistoryDao().getLatestScan()
            if (latestScan != null) {
                _scanState.value = _scanState.value.copy(
                    lastScanTimestamp = latestScan.timestamp,
                    totalCacheFound = latestScan.totalCacheBytes,
                    userAppsCache = latestScan.userAppsCacheBytes,
                    systemAppsCache = latestScan.systemAppsCacheBytes
                )
            }

            val currentSettings = settingsRepo.settingsFlow.firstOrNull() ?: AppSettings()
            if (currentSettings.scanOnAppOpen) {
                startScan()
            }
        }
    }

    fun checkUsagePermission() {
        val granted = scanner.hasUsageStatsPermission()
        _scanState.value = _scanState.value.copy(hasUsagePermission = granted)
    }

    fun startScan() {
        if (_scanState.value.isScanning) return

        viewModelScope.launch {
            checkUsagePermission()
            scanner.scanAppsFlow().collect { event ->
                when (event) {
                    is ScanProgressEvent.Started -> {
                        _scanState.value = _scanState.value.copy(
                            isScanning = true,
                            progressIndex = 0,
                            totalCount = event.totalApps,
                            currentAppName = "Iniciando varredura..."
                        )
                    }
                    is ScanProgressEvent.AppScanned -> {
                        _scanState.value = _scanState.value.copy(
                            progressIndex = event.currentIndex,
                            totalCount = event.totalApps,
                            currentAppName = event.app.appName,
                            totalCacheFound = event.currentTotalCache
                        )
                    }
                    is ScanProgressEvent.Completed -> {
                        val timestamp = System.currentTimeMillis()
                        _scanState.value = _scanState.value.copy(
                            isScanning = false,
                            allApps = event.apps,
                            totalCacheFound = event.totalCacheBytes,
                            userAppsCache = event.userAppsCacheBytes,
                            systemAppsCache = event.systemAppsCacheBytes,
                            lastScanTimestamp = timestamp
                        )

                        // Save scan record to Room
                        db.scanHistoryDao().insertScan(
                            ScanHistoryEntity(
                                timestamp = timestamp,
                                totalCacheBytes = event.totalCacheBytes,
                                userAppsCacheBytes = event.userAppsCacheBytes,
                                systemAppsCacheBytes = event.systemAppsCacheBytes,
                                appsScanned = event.apps.size,
                                appsWithCache = event.apps.count { it.hasCache }
                            )
                        )

                        // Check notifications
                        val currentSettings = settings.value
                        if (currentSettings.notificationsEnabled && currentSettings.notifyOnScanComplete) {
                            NotificationHelper.notifyScanComplete(
                                getApplication(),
                                event.totalCacheBytes,
                                event.apps.count { it.hasCache }
                            )
                        }
                    }
                    is ScanProgressEvent.Failed -> {
                        _scanState.value = _scanState.value.copy(
                            isScanning = false,
                            currentAppName = "Erro: ${event.error}"
                        )
                    }
                }
            }
        }
    }

    fun cleanAllApps() {
        val eligibleApps = _scanState.value.allApps.filter { it.isEligibleForClean }
        if (eligibleApps.isEmpty()) return

        executeCleaning(eligibleApps, CleanType.ALL)
    }

    fun cleanSelectedApps() {
        val selectedPackages = _selectedPackageNames.value
        val targetApps = _scanState.value.allApps.filter {
            selectedPackages.contains(it.packageName) && it.isEligibleForClean
        }
        if (targetApps.isEmpty()) return

        executeCleaning(targetApps, CleanType.SELECTIVE)
    }

    fun cleanSingleApp(app: AppCacheInfo) {
        executeCleaning(listOf(app), CleanType.INDIVIDUAL)
    }

    private fun executeCleaning(apps: List<AppCacheInfo>, type: CleanType) {
        if (_cleaningProgress.value.isRunning) return

        viewModelScope.launch {
            cleaner.executeClean(apps, type).collect { (progress, report) ->
                _cleaningProgress.value = progress
                if (report != null) {
                    _latestReport.value = report
                    _showReportDialog.value = true

                    val currentSettings = settings.value
                    if (currentSettings.notificationsEnabled && currentSettings.notifyOnCleanComplete) {
                        NotificationHelper.notifyCleanComplete(
                            getApplication(),
                            report.totalBytesCleaned,
                            report.appsCleaned
                        )
                    }

                    // Refresh scan results to reflect freed space
                    startScan()
                    _selectedPackageNames.value = emptySet()
                }
            }
        }
    }

    fun toggleSelectApp(packageName: String) {
        val current = _selectedPackageNames.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackageNames.value = current
    }

    fun selectAll(apps: List<AppCacheInfo>) {
        _selectedPackageNames.value = apps.filter { it.isEligibleForClean }.map { it.packageName }.toSet()
    }

    fun deselectAll() {
        _selectedPackageNames.value = emptySet()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterOnlyWithCache(enabled: Boolean) {
        _filterOnlyWithCache.value = enabled
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun selectAppForDetail(app: AppCacheInfo?) {
        _selectedAppForDetail.value = app
    }

    fun toggleAppProtection(app: AppCacheInfo) {
        viewModelScope.launch {
            val isExcluded = db.excludedAppDao().isExcluded(app.packageName)
            if (isExcluded) {
                db.excludedAppDao().removeByPackage(app.packageName)
            } else {
                db.excludedAppDao().insertExcluded(
                    ExcludedAppEntity(
                        packageName = app.packageName,
                        appName = app.appName
                    )
                )
            }
            // Update local state
            val updatedApps = _scanState.value.allApps.map {
                if (it.packageName == app.packageName) {
                    it.copy(isProtected = !isExcluded)
                } else it
            }
            _scanState.value = _scanState.value.copy(allApps = updatedApps)
            _selectedAppForDetail.value = _selectedAppForDetail.value?.let {
                if (it.packageName == app.packageName) it.copy(isProtected = !isExcluded) else it
            }
        }
    }

    fun dismissReportDialog() {
        _showReportDialog.value = false
    }

    fun showReport(report: CleanReport) {
        _latestReport.value = report
        _showReportDialog.value = true
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.cleanHistoryDao().clearAll()
        }
    }

    fun reconnectShizuku() {
        shizukuManager.checkStatus()
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun openShizukuApp() {
        shizukuManager.openShizukuApp()
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepo.updateThemeMode(themeMode)
        }
    }

    fun updateAutoCleanSettings(
        frequency: AutoCleanFrequency,
        hour: Int,
        minute: Int,
        target: AutoCleanTarget,
        requireConfirmation: Boolean
    ) {
        viewModelScope.launch {
            settingsRepo.updateAutoCleanSettings(frequency, hour, minute, target, requireConfirmation)
            WorkScheduler.scheduleOrCancel(getApplication(), frequency, hour, minute)
        }
    }

    fun updateNotifications(enabled: Boolean, scan: Boolean, clean: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateNotifications(enabled, scan, clean)
        }
    }

    fun updateGeneralSettings(
        scanOnOpen: Boolean,
        autoRefresh: Boolean,
        showSystemApps: Boolean,
        showTechInfo: Boolean,
        autoClearDays: Int
    ) {
        viewModelScope.launch {
            settingsRepo.updateGeneralSettings(scanOnOpen, autoRefresh, showSystemApps, showTechInfo, autoClearDays)
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            settingsRepo.restoreDefaults()
            WorkScheduler.scheduleOrCancel(getApplication(), AutoCleanFrequency.OFF, 3, 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.destroy()
    }
}
