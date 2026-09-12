package com.notificationhistory.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notificationhistory.domain.model.AppFilterItem
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.ui.components.AccessRevokedBanner
import com.notificationhistory.ui.components.AppFilterBottomSheet
import com.notificationhistory.ui.components.DisabledSetupView
import com.notificationhistory.ui.components.NotificationCard
import com.notificationhistory.ui.components.NotificationMetadataSection
import com.notificationhistory.ui.components.SearchTopBar
import com.notificationhistory.ui.components.ServiceStatusHeader
import com.notificationhistory.util.SettingsNavigationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    viewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    ),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.isSearchActive) {
        viewModel.setSearchActive(false)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshState()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoClear()
            }
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (!uiState.isSearchActive) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Notification History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (uiState.notifications.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearAll() }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all notifications",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SearchTopBar(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onClearQuery = { viewModel.clearSearchQuery() },
                            onCloseSearch = { viewModel.setSearchActive(false) },
                            onFilterClick = { viewModel.setAppFilterSheetVisible(true) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ServiceStatusHeader(
                listenerState = uiState.listenerState,
                capturedCount = uiState.capturedCount,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            )

            if (uiState.isSearchActive) {
                // Active Filter Pill Row
                val selectedPkg = uiState.selectedAppFilter
                if (selectedPkg != null) {
                    val selectedFilterItem = remember(uiState.availableAppFilters, selectedPkg) {
                        uiState.availableAppFilters.firstOrNull { it.packageName == selectedPkg }
                    }
                    val displayAppName: String = remember(selectedFilterItem, selectedPkg) {
                        selectedFilterItem?.appName?.takeIf { it.isNotBlank() } ?: runCatching {
                            val appInfo = context.packageManager.getApplicationInfo(selectedPkg, 0)
                            context.packageManager.getApplicationLabel(appInfo).toString()
                        }.getOrNull() ?: selectedPkg
                    }
                    val appIcon = remember(selectedPkg) {
                        runCatching {
                            val drawable = context.packageManager.getApplicationIcon(selectedPkg)
                            drawable.toBitmap(width = 48, height = 48).asImageBitmap()
                        }.getOrNull()
                    }
                    val initial = remember(displayAppName) {
                        displayAppName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.setAppFilterSheetVisible(true) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (appIcon != null) {
                                            Image(
                                                bitmap = appIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = initial,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = displayAppName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Change app filter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(2.dp))

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.clearAppFilter() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear app filter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        TextButton(
                            onClick = { viewModel.clearAppFilter() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "All Apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Match Summary Row
                if (uiState.searchQuery.isNotBlank() || uiState.selectedAppFilter != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.searchQuery.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "${uiState.filteredNotifications.size} matches for query ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Text(
                                        text = "\"${uiState.searchQuery}\"",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "${uiState.filteredNotifications.size} notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.selectedAppFilter != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filtered by 1 app",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (uiState.listenerState == ListenerState.Inactive) {
                DisabledSetupView(
                    onOpenSettings = {
                        SettingsNavigationHelper.openNotificationListenerSettings(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.listenerState == ListenerState.Paused) {
                        item(key = "paused_banner") {
                            AccessRevokedBanner(
                                onFixClick = {
                                    SettingsNavigationHelper.openNotificationListenerSettings(context)
                                }
                            )
                        }
                    }

                    if (uiState.filteredNotifications.isEmpty()) {
                        item(key = "empty_view") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isSearchActive) {
                                                Icons.Default.SearchOff
                                            } else {
                                                Icons.Default.NotificationsPaused
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text(
                                        text = if (uiState.isSearchActive) {
                                            "No matching notifications"
                                        } else {
                                            "No notifications"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (uiState.isSearchActive) {
                                            "Try searching for a different keyword or changing the app filter."
                                        } else {
                                            "New notifications will appear here automatically from the listener service."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = uiState.filteredNotifications,
                            key = { it.id }
                        ) { record ->
                            NotificationCard(
                                record = record,
                                isExpanded = uiState.expandedCardIds.contains(record.id),
                                onToggleExpand = { viewModel.toggleCardExpansion(record.id) },
                                searchQuery = if (uiState.isSearchActive) uiState.searchQuery else "",
                                metadataContent = { NotificationMetadataSection(record = record) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAppFilterSheetVisible) {
        AppFilterBottomSheet(
            availableAppFilters = uiState.availableAppFilters,
            selectedAppPackage = uiState.selectedAppFilter,
            sheetSearchQuery = uiState.appFilterSearchQuery,
            totalNotifications = uiState.capturedCount,
            onSheetSearchQueryChange = { viewModel.onAppFilterSearchQueryChanged(it) },
            onApplyFilter = { viewModel.selectAppFilter(it) },
            onDismiss = { viewModel.setAppFilterSheetVisible(false) }
        )
    }
}

