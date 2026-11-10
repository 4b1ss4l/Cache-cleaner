package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.CleanHistoryEntity
import com.example.model.AppCleanDetail
import com.example.model.CleanOutcome
import com.example.model.CleanReport
import com.example.model.CleanType
import com.example.ui.MainViewModel
import com.example.ui.components.CleaningReportDialog
import com.example.ui.theme.CleanError
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanWarning
import com.example.util.FormatUtils
import org.json.JSONArray

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.cleanHistory.collectAsState()
    val totalFreed by viewModel.totalFreedBytes.collectAsState()
    val sessionCount by viewModel.totalSessionsCount.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedReportForModal by remember { mutableStateOf<CleanReport?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Histórico de Limpezas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$sessionCount sessões registradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (historyList.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpar histórico",
                        tint = CleanError
                    )
                }
            }
        }

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_summary_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Acumulado Liberado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatBytes(totalFreed ?: 0L),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanSuccess
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CleanSuccess.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = CleanSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma limpeza realizada ainda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Execute uma análise e limpeza no dashboard para registrar histórico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        entity = item,
                        onClick = {
                            selectedReportForModal = parseEntityToReport(item)
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Limpar Histórico") },
            text = { Text("Deseja apagar todos os registros de limpezas anteriores? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearHistory()
                    },
                    modifier = Modifier.testTag("confirm_clear_history_btn")
                ) {
                    Text("Limpar", color = CleanError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    selectedReportForModal?.let { report ->
        CleaningReportDialog(
            report = report,
            onDismiss = { selectedReportForModal = null }
        )
    }
}

@Composable
private fun HistoryItemCard(
    entity: CleanHistoryEntity,
    onClick: () -> Unit
) {
    val typeLabel = when (entity.cleanType) {
        CleanType.ALL.name -> "Limpeza Completa"
        CleanType.SELECTIVE.name -> "Limpeza Seletiva"
        CleanType.INDIVIDUAL.name -> "Limpeza Individual"
        CleanType.AUTO.name -> "Limpeza Automática"
        else -> entity.cleanType
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${entity.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (entity.wasPrivileged) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shizuku",
                            style = MaterialTheme.typography.labelSmall,
                            color = CleanSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = FormatUtils.formatTimestamp(entity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${entity.appsCleaned} de ${entity.appsProcessed} apps limpos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatBytes(entity.totalBytesCleaned),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CleanSuccess
                    )
                    Text(
                        text = "liberados",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver Detalhes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun parseEntityToReport(entity: CleanHistoryEntity): CleanReport {
    val details = ArrayList<AppCleanDetail>()
    try {
        val array = JSONArray(entity.detailsJson)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val outcomeStr = obj.optString("outcome", CleanOutcome.SUCCESS.name)
            val outcome = runCatching { CleanOutcome.valueOf(outcomeStr) }.getOrDefault(CleanOutcome.SUCCESS)
            details.add(
                AppCleanDetail(
                    packageName = obj.optString("packageName"),
                    appName = obj.optString("appName"),
                    cacheBytesBefore = obj.optLong("cacheBytesBefore"),
                    cacheBytesCleaned = obj.optLong("cacheBytesCleaned"),
                    outcome = outcome,
                    message = obj.optString("message")
                )
            )
        }
    } catch (_: Exception) {}

    val cleanType = runCatching { CleanType.valueOf(entity.cleanType) }.getOrDefault(CleanType.ALL)

    return CleanReport(
        timestamp = entity.timestamp,
        type = cleanType,
        appsProcessed = entity.appsProcessed,
        appsCleaned = entity.appsCleaned,
        appsSkipped = entity.appsSkipped,
        appsFailed = entity.appsFailed,
        totalBytesCleaned = entity.totalBytesCleaned,
        details = details,
        wasPrivileged = entity.wasPrivileged
    )
}
