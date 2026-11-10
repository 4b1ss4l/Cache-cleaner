package com.example.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AutoCleanFrequency
import com.example.model.AutoCleanTarget
import com.example.model.ThemeMode
import com.example.ui.MainViewModel
import com.example.ui.components.ShizukuGuideDialog
import com.example.ui.components.ShizukuStatusCard
import com.example.util.FormatUtils

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()

    var showShizukuGuide by remember { mutableStateOf(false) }
    var showFreqMenu by remember { mutableStateOf(false) }
    var showTargetMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 1. APARÊNCIA
        SectionHeader(icon = Icons.Default.Palette, title = "Aparência")
        Card(
            modifier = Modifier.fillMaxWidth().testTag("theme_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ThemeOptionRow("Padrão do sistema", ThemeMode.SYSTEM, settings.themeMode) {
                    viewModel.updateThemeMode(ThemeMode.SYSTEM)
                }
                ThemeOptionRow("Branco (Interface clara)", ThemeMode.LIGHT, settings.themeMode) {
                    viewModel.updateThemeMode(ThemeMode.LIGHT)
                }
                ThemeOptionRow("Preto (Interface escura)", ThemeMode.DARK, settings.themeMode) {
                    viewModel.updateThemeMode(ThemeMode.DARK)
                }
                ThemeOptionRow("OLED Black (Preto puro AMOLED)", ThemeMode.OLED_BLACK, settings.themeMode) {
                    viewModel.updateThemeMode(ThemeMode.OLED_BLACK)
                }
            }
        }

        // 2. SHIZUKU
        SectionHeader(icon = Icons.Default.Security, title = "Shizuku & Privilégios")
        ShizukuStatusCard(
            status = shizukuStatus,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onReconnect = { viewModel.reconnectShizuku() },
            onOpenGuide = { showShizukuGuide = true }
        )

        // 3. LIMPEZA AUTOMÁTICA
        SectionHeader(icon = Icons.Default.Schedule, title = "Limpeza Automática")
        Card(
            modifier = Modifier.fillMaxWidth().testTag("autoclean_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Frequência de Limpeza", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = settings.autoCleanFrequency.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        OutlinedButton(
                            onClick = { showFreqMenu = true },
                            modifier = Modifier.testTag("freq_dropdown_btn")
                        ) {
                            Text("Alterar")
                        }
                        DropdownMenu(expanded = showFreqMenu, onDismissRequest = { showFreqMenu = false }) {
                            AutoCleanFrequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.label) },
                                    onClick = {
                                        viewModel.updateAutoCleanSettings(
                                            freq,
                                            settings.autoCleanHour,
                                            settings.autoCleanMinute,
                                            settings.autoCleanTarget,
                                            settings.autoCleanRequireConfirmation
                                        )
                                        showFreqMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (settings.autoCleanFrequency != AutoCleanFrequency.OFF) {
                    HorizontalDivider()

                    // Time of Day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Horário de Execução", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = FormatUtils.formatShortTime(settings.autoCleanHour, settings.autoCleanMinute),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        viewModel.updateAutoCleanSettings(
                                            settings.autoCleanFrequency,
                                            hour,
                                            minute,
                                            settings.autoCleanTarget,
                                            settings.autoCleanRequireConfirmation
                                        )
                                    },
                                    settings.autoCleanHour,
                                    settings.autoCleanMinute,
                                    true
                                ).show()
                            },
                            modifier = Modifier.testTag("time_picker_btn")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajustar")
                        }
                    }

                    HorizontalDivider()

                    // Target Apps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Aplicativos Alvo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = settings.autoCleanTarget.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            OutlinedButton(onClick = { showTargetMenu = true }) {
                                Text("Alterar")
                            }
                            DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                                AutoCleanTarget.values().forEach { target ->
                                    DropdownMenuItem(
                                        text = { Text(target.label) },
                                        onClick = {
                                            viewModel.updateAutoCleanSettings(
                                                settings.autoCleanFrequency,
                                                settings.autoCleanHour,
                                                settings.autoCleanMinute,
                                                target,
                                                settings.autoCleanRequireConfirmation
                                            )
                                            showTargetMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Require Confirmation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Exigir Confirmação", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Notificar antes de executar a limpeza", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoCleanRequireConfirmation,
                            onCheckedChange = {
                                viewModel.updateAutoCleanSettings(
                                    settings.autoCleanFrequency,
                                    settings.autoCleanHour,
                                    settings.autoCleanMinute,
                                    settings.autoCleanTarget,
                                    it
                                )
                            }
                        )
                    }

                    if (settings.lastAutoCleanResult.isNotEmpty()) {
                        HorizontalDivider()
                        Column {
                            Text(text = "Última Execução Automática:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${FormatUtils.formatTimestamp(settings.lastAutoCleanTimestamp)} — ${settings.lastAutoCleanResult}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. NOTIFICAÇÕES
        SectionHeader(icon = Icons.Default.Notifications, title = "Notificações")
        Card(
            modifier = Modifier.fillMaxWidth().testTag("notifications_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingSwitchRow(
                    title = "Ativar Notificações",
                    subtitle = "Alertas de varredura e limpeza",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = {
                        viewModel.updateNotifications(it, settings.notifyOnScanComplete, settings.notifyOnCleanComplete)
                    }
                )
                if (settings.notificationsEnabled) {
                    SettingSwitchRow(
                        title = "Notificar ao Concluir Varredura",
                        subtitle = "Informa cache total identificado",
                        checked = settings.notifyOnScanComplete,
                        onCheckedChange = {
                            viewModel.updateNotifications(settings.notificationsEnabled, it, settings.notifyOnCleanComplete)
                        }
                    )
                    SettingSwitchRow(
                        title = "Notificar ao Concluir Limpeza",
                        subtitle = "Informa quantidade de espaço liberado",
                        checked = settings.notifyOnCleanComplete,
                        onCheckedChange = {
                            viewModel.updateNotifications(settings.notificationsEnabled, settings.notifyOnScanComplete, it)
                        }
                    )
                }
            }
        }

        // 5. OUTRAS CONFIGURAÇÕES
        SectionHeader(icon = Icons.Default.Tune, title = "Comportamento e Preferências")
        Card(
            modifier = Modifier.fillMaxWidth().testTag("general_settings_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingSwitchRow(
                    title = "Iniciar Varredura ao Abrir",
                    subtitle = "Analisa o cache automaticamente na abertura",
                    checked = settings.scanOnAppOpen,
                    onCheckedChange = {
                        viewModel.updateGeneralSettings(it, settings.autoRefreshResults, settings.showSystemApps, settings.showTechnicalInfo, settings.autoClearHistoryDays)
                    }
                )
                SettingSwitchRow(
                    title = "Mostrar Informações Técnicas",
                    subtitle = "Exibe UID, Target SDK e diretório nos detalhes",
                    checked = settings.showTechnicalInfo,
                    onCheckedChange = {
                        viewModel.updateGeneralSettings(settings.scanOnAppOpen, settings.autoRefreshResults, settings.showSystemApps, it, settings.autoClearHistoryDays)
                    }
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restaurar Configurações Padrão",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { showRestoreConfirm = true },
                        modifier = Modifier.testTag("restore_defaults_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar")
                    }
                }
            }
        }

        // 6. SOBRE O APLICATIVO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAboutDialog = true }
                .testTag("about_app_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Sobre o Cache Cleaner", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Versão 1.0 • Desenvolvido com Shizuku API", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showShizukuGuide) {
        ShizukuGuideDialog(
            onDismiss = { showShizukuGuide = false },
            onOpenWebsite = { viewModel.shizukuManager.openShizukuWebsite() },
            onOpenApp = { viewModel.openShizukuApp() }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar Configurações") },
            text = { Text("Deseja restaurar todas as configurações para os valores de fábrica?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        viewModel.restoreDefaults()
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Cache Cleaner — Intelligent Manager") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aplicativo completo para gerenciamento e limpeza inteligente de cache Android.")
                    Text("Integração segura via Shizuku API sem necessidade de root, preservando suas contas, credenciais e arquivos pessoais.")
                    Text("Versão: 1.0 (Build 2026)", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    currentMode: ThemeMode,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = currentMode == mode,
            onClick = onSelect,
            modifier = Modifier.testTag("theme_radio_${mode.name}")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
