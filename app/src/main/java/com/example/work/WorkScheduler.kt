package com.example.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.model.AutoCleanFrequency
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val UNIQUE_WORK_NAME = "cache_cleaner_periodic_work"

    fun scheduleOrCancel(
        context: Context,
        frequency: AutoCleanFrequency,
        targetHour: Int,
        targetMinute: Int
    ) {
        val workManager = WorkManager.getInstance(context)

        if (frequency == AutoCleanFrequency.OFF) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val repeatIntervalDays = frequency.days.toLong().coerceAtLeast(1L)

        // Calculate initial delay to target hour:minute
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMinutes = ((target.timeInMillis - now.timeInMillis) / 1000 / 60).coerceAtLeast(1L)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<AutoCleanWorker>(
            repeatIntervalDays,
            TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }
}
