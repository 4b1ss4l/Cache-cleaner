package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppCacheInfo
import com.example.ui.MainViewModel
import com.example.ui.SortOrder
import com.example.ui.components.AppDetailDialog
import com.example.ui.components.AppItemCard
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val scanState by viewModel.scanState.collectAsState()
    val selectedPackages by viewModel.selectedPackageNames.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterOnlyWithCache by viewModel.filterOnlyWithCache.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: User apps, 1: System apps
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedAppForModal by remember { mutableStateOf<AppCacheInfo?>(null) }

    val userApps = remember(scanState.allApps) {
        scanState.allApps.filter { !it.isSystemApp }
    }
    val systemApps = remember(scanState.allApps) {
        scanState.allApps.filter { it.isSystemApp }
    }

    val currentTabApps = if (selectedTabIndex == 0) userApps else systemApps

    val filteredApps = remember(currentTabApps, searchQuery, filterOnlyWithCache, sortOrder) {
        currentTabApps
            .filter { app ->
                val matchesQuery = searchQuery.isBlank() ||
                        app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
                val matchesCache = !filterOnlyWithCache || app.hasCache
                matchesQuery && matchesCache
            }
            .let { list ->
                when (sortOrder) {
                    SortOrder.LARGEST_CACHE -> list.sortedByDescending { it.cacheBytes }
                    SortOrder.NAME_ASC -> list.sortedBy { it.appName.lowercase() }
                    SortOrder.NAME_DESC -> list.sortedByDescending { it.appName.lowercase() }
                }
            }
    }

    val selectedTargetApps = remember(scanState.allApps, selectedPackages) {
        scanState.allApps.filter { selectedPackages.contains(it.packageName) && it.isEligibleForClean }
    }
    val selectedTotalCache = selectedTargetApps.sumOf { it.cacheBytes }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category Tabs: Usuário vs Sistema
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth().testTag("apps_category_tabs")
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Usuário (${userApps.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_user_apps")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Sistema (${systemApps.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_system_apps")
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar por nome ou pacote...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apps_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filters and Selection Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = filterOnlyWithCache,
                            onClick = { viewModel.setFilterOnlyWithCache(!filterOnlyWithCache) },
                            label = { Text("Com Cache") },
                            leadingIcon = if (filterOnlyWithCache) {
                                { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("filter_cache_chip")
                        )

                        // Sort order button
                        Box {
                            TextButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_btn")
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    when (sortOrder) {
                                        SortOrder.LARGEST_CACHE -> "Maior Cache"
                                        SortOrder.NAME_ASC -> "Nome (A-Z)"
                                        SortOrder.NAME_DESC -> "Nome (Z-A)"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Maior Cache") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.LARGEST_CACHE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nome (A-Z)") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NAME_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nome (Z-A)") },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NAME_DESC)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Bulk selection actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.selectAll(filteredApps) },
                            modifier = Modifier.testTag("select_all_btn")
                        ) {
                            Text("Todos")
                        }
                        if (selectedPackages.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.deselectAll() },
                                modifier = Modifier.testTag("deselect_all_btn")
                            ) {
                                Text("Limpar")
                            }
                        }
                    }
                }
            }

            // List of applications
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nenhum aplicativo encontrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tente alterar os termos da busca ou os filtros.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .testTag("apps_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItemCard(
                            app = app,
                            isSelected = selectedPackages.contains(app.packageName),
                            onToggleSelect = { viewModel.toggleSelectApp(app.packageName) },
                            onClick = { selectedAppForModal = app }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }

        // Floating Action Bar for Selective Cleaning
        if (selectedTargetApps.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("selective_clean_bar"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${selectedTargetApps.size} app(s) selecionado(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cache total: ${FormatUtils.formatBytes(selectedTotalCache)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Button(
                        onClick = { viewModel.cleanSelectedApps() },
                        modifier = Modifier.testTag("clean_selected_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpar (${selectedTargetApps.size})")
                    }
                }
            }
        }
    }

    selectedAppForModal?.let { app ->
        AppDetailDialog(
            app = app,
            showTechInfo = settings.showTechnicalInfo,
            onDismiss = { selectedAppForModal = null },
            onCleanIndividual = { viewModel.cleanSingleApp(it) },
            onToggleProtection = { viewModel.toggleAppProtection(it) }
        )
    }
}
