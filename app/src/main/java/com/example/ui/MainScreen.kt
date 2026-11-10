package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.components.CleaningProgressDialog
import com.example.ui.components.CleaningReportDialog
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CacheCleanerTheme

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val cleaningProgress by viewModel.cleaningProgress.collectAsState()
    val latestReport by viewModel.latestReport.collectAsState()
    val showReportDialog by viewModel.showReportDialog.collectAsState()

    var selectedNavIndex by rememberSaveable { mutableIntStateOf(0) }

    // BackHandler to navigate back to Home/Dashboard if in a sub-tab
    BackHandler(enabled = selectedNavIndex != 0) {
        selectedNavIndex = 0
    }

    val navItems = listOf(
        NavItem("Início", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
        NavItem("Aplicativos", Icons.Filled.Apps, Icons.Outlined.Apps, "nav_apps"),
        NavItem("Histórico", Icons.Filled.History, Icons.Outlined.History, "nav_history"),
        NavItem("Configurações", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    CacheCleanerTheme(themeMode = settings.themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedNavIndex == index,
                            onClick = { selectedNavIndex = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedNavIndex == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedNavIndex) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    scanState = scanState,
                    shizukuStatus = shizukuStatus,
                    onNavigateToApps = { selectedNavIndex = 1 },
                    onNavigateToSettings = { selectedNavIndex = 3 },
                    onNavigateToHistory = { selectedNavIndex = 2 },
                    modifier = Modifier.padding(innerPadding)
                )
                1 -> AppsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                2 -> HistoryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // Global real-time cleaning progress dialog
            CleaningProgressDialog(progress = cleaningProgress)

            // Global post-cleaning completion report dialog
            if (showReportDialog && latestReport != null) {
                CleaningReportDialog(
                    report = latestReport!!,
                    onDismiss = { viewModel.dismissReportDialog() }
                )
            }
        }
    }
}
