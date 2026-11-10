package com.example.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    OLED_BLACK
}

enum class AutoCleanFrequency(val days: Int, val label: String) {
    OFF(0, "Desativada"),
    DAILY(1, "Diariamente"),
    EVERY_3_DAYS(3, "A cada 3 dias"),
    WEEKLY(7, "Semanalmente"),
    EVERY_15_DAYS(15, "A cada 15 dias"),
    MONTHLY(30, "Mensalmente")
}

enum class AutoCleanTarget(val label: String) {
    USER_ONLY("Apenas aplicativos do usuário"),
    SYSTEM_ONLY("Apenas aplicativos do sistema"),
    BOTH("Usuário e sistema")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoCleanFrequency: AutoCleanFrequency = AutoCleanFrequency.OFF,
    val autoCleanHour: Int = 3,
    val autoCleanMinute: Int = 0,
    val autoCleanTarget: AutoCleanTarget = AutoCleanTarget.BOTH,
    val autoCleanRequireConfirmation: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notifyOnScanComplete: Boolean = true,
    val notifyOnCleanComplete: Boolean = true,
    val scanOnAppOpen: Boolean = true,
    val autoRefreshResults: Boolean = false,
    val showSystemApps: Boolean = true,
    val showTechnicalInfo: Boolean = false,
    val autoClearHistoryDays: Int = 30,
    val lastAutoCleanTimestamp: Long = 0L,
    val lastAutoCleanResult: String = ""
)
