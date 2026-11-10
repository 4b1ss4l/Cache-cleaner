package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ShizukuStatus
import com.example.ui.theme.CleanError
import com.example.ui.theme.CleanSuccess
import com.example.ui.theme.CleanWarning

@Composable
fun ShizukuStatusCard(
    status: ShizukuStatus,
    onRequestPermission: () -> Unit,
    onReconnect: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusIcon, statusDesc) = when (status) {
        is ShizukuStatus.Authorized -> Triple(
            CleanSuccess,
            Icons.Default.CheckCircle,
            "Privilégios ativos (v${status.version} • UID ${status.uid}). Limpezas automáticas e em massa em segundo plano habilitadas com sucesso."
        )
        is ShizukuStatus.Unauthorized -> Triple(
            CleanWarning,
            Icons.Default.Security,
            "O serviço Shizuku está ativo! Se você já ativou o app dentro do Shizuku, toque em 'Atualizar Estado' abaixo. Caso contrário, toque em 'Solicitar Autorização'."
        )
        is ShizukuStatus.ServiceStopped -> Triple(
            CleanWarning,
            Icons.Default.Warning,
            "Shizuku instalado, mas o serviço ainda não foi iniciado via Depuração sem fio ou ADB."
        )
        is ShizukuStatus.NotInstalled -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info,
            "Shizuku não detectado. O app funcionará com métodos padrão do Android."
        )
        is ShizukuStatus.Checking -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Refresh,
            "Verificando status da conexão com Shizuku..."
        )
        is ShizukuStatus.Error -> Triple(
            CleanError,
            Icons.Default.Warning,
            "Erro de comunicação com Shizuku: ${status.message}"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status Shizuku",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Shizuku Privilegiado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = onOpenGuide,
                    modifier = Modifier.testTag("shizuku_guide_btn")
                ) {
                    Text("Guia")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (status) {
                is ShizukuStatus.Unauthorized -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_authorize_btn")
                        ) {
                            Text("Solicitar Autorização")
                        }
                        FilledTonalButton(
                            onClick = onReconnect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_recheck_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Atualizar Estado")
                        }
                    }
                }
                is ShizukuStatus.Authorized -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onReconnect,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shizuku_check_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verificar Conexão")
                        }
                    }
                }
                is ShizukuStatus.ServiceStopped -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = onReconnect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_reconnect_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verificar")
                        }
                        OutlinedButton(
                            onClick = onOpenGuide,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_guide_alt_btn")
                        ) {
                            Text("Como Iniciar")
                        }
                    }
                }
                is ShizukuStatus.NotInstalled -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onOpenGuide,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_setup_btn")
                        ) {
                            Text("Como Instalar")
                        }
                        FilledTonalButton(
                            onClick = onReconnect,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shizuku_retry_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verificar")
                        }
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onReconnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shizuku_check_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Atualizar Estado")
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuGuideDialog(
    onDismiss: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guia e Integração com Shizuku")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "O Shizuku permite que o Cache Cleaner execute operações privilegiadas (como limpeza em massa e tarefas em segundo plano) sem precisar de acesso Root.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Como ativar e autorizar:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "1. Abra o app oficial Shizuku no seu dispositivo.\n" +
                            "2. Verifique se o serviço Shizuku está 'Em execução' (via Depuração sem fio ou ADB).\n" +
                            "3. No app Shizuku, toque em 'Aplicativos autorizados' e ative a chave ao lado do 'Cache Cleaner'.\n" +
                            "4. Retorne ao Cache Cleaner e toque em 'Atualizar Estado' no painel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Compatibilidade e Segurança:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "O Cache Cleaner NÃO apaga contas, senhas ou dados salvos. Apenas limpa arquivos temporários de cache dispensáveis. Sem o Shizuku, o app ainda permite análise completa de armazenamento e acesso direto às configurações de cada app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenApp,
                modifier = Modifier.testTag("dialog_open_shizuku_btn")
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir Shizuku")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onOpenWebsite,
                    modifier = Modifier.testTag("dialog_site_shizuku_btn")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Site Oficial")
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        }
    )
}
