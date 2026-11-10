package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) {
            String.format(Locale.getDefault(), "%d %s", bytes, units[index])
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[index])
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Nunca"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Nunca realizado"
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        if (diffMs < 0) return formatTimestamp(timestamp)

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Agora há pouco"
            minutes < 60 -> "Há $minutes min"
            hours < 24 -> "Há $hours h"
            days < 7 -> "Há $days d"
            else -> formatTimestamp(timestamp)
        }
    }
}
