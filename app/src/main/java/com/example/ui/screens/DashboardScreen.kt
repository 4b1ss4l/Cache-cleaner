package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppCacheInfo
import com.example.model.ShizukuStatus
import com.example.ui.MainViewModel
import com.example.ui.components.AppDetailDialog
import com.example.ui.components.AppItemCard
import com.example.ui.components.ShizukuGuideDialog
import com.example.ui.components.ShizukuStatusCard
import com.example.ui.components.StorageOverviewCard
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanWarning
import com.example.ui.theme.TealPrimary
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    scanState: com.example.ui.ScanUiState,
    shizukuStatus: ShizukuStatus,
    onNavigateToApps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showShizukuGuide by remember { mutableStateOf(false) }
    var selectedAppForModal by remember { mutableStateOf<AppCacheInfo?>(null) }

    val topCacheHogs = remember(scanState.allApps) {
        scanState.allApps.filter { it.hasCache }.sortedByDescending { it.cacheBytes }.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cache_cleaner_fg),
                        contentDescription = "Ícone Cache Cleaner",
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Cache Cleaner",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gerenciador Inteligente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shizuku quick status chip
                val statusChipColor = when (shizukuStatus) {
                    is ShizukuStatus.Authorized -> CleanSuccess
                    is ShizukuStatus.Checking -> MaterialTheme.colorScheme.primary
                    else -> CleanWarning
                }

                AssistChip(
                    onClick = { showShizukuGuide = true },
                    label = {
                        Text(
                            text = if (shizukuStatus is ShizukuStatus.Authorized) "Shizuku Conectado" else "Shizuku",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusChipColor
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusChipColor)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusChipColor.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("dashboard_shizuku_chip")
                )

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("dashboard_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configurações")
                }
            }
        }

        // Storage Overview Card with live scanning and action buttons
        StorageOverviewCard(
            scanState = scanState,
            onStartScan = { viewModel.startScan() },
            onCleanAll = { viewModel.cleanAllApps() }
        )

        // Shizuku Status & Privileges Card
        ShizukuStatusCard(
            status = shizukuStatus,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onReconnect = { viewModel.reconnectShizuku() },
            onOpenGuide = { showShizukuGuide = true }
        )

        // Smart Insights & Top Hogs Card
        if (topCacheHogs.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_insights_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Análise Inteligente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = onNavigateToApps,
                            modifier = Modifier.testTag("view_all_apps_btn")
                        ) {
                            Text("Ver Todos")
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = "Aplicativos com maior acúmulo de cache dispensável no dispositivo:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topCacheHogs.forEach { app ->
                            AppItemCard(
                                app = app,
                                isSelected = viewModel.selectedPackageNames.value.contains(app.packageName),
                                onToggleSelect = { viewModel.toggleSelectApp(app.packageName) },
                                onClick = { selectedAppForModal = app }
                            )
                        }
                    }
                }
            }
        }

        // Quick Navigation Tiles (Gerenciador e Histórico)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToApps)
                    .testTag("nav_to_apps_tile"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gerenciar Apps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Seleção e filtros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onNavigateToHistory)
                    .testTag("nav_to_history_tile"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = CleanSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Histórico",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sessões e relatórios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showShizukuGuide) {
        ShizukuGuideDialog(
            onDismiss = { showShizukuGuide = false },
            onOpenWebsite = { viewModel.shizukuManager.openShizukuWebsite() },
            onOpenApp = { viewModel.openShizukuApp() }
        )
    }

    selectedAppForModal?.let { app ->
        AppDetailDialog(
            app = app,
            showTechInfo = viewModel.settings.value.showTechnicalInfo,
            onDismiss = { selectedAppForModal = null },
            onCleanIndividual = { viewModel.cleanSingleApp(it) },
            onToggleProtection = { viewModel.toggleAppProtection(it) }
        )
    }
}
