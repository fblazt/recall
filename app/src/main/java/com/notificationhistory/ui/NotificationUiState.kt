package com.notificationhistory.ui

import com.notificationhistory.domain.model.AppFilterItem
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.domain.model.NotificationRecord

data class NotificationUiState(
    val notifications: List<NotificationRecord> = emptyList(),
    val listenerState: ListenerState = ListenerState.Inactive,
    val capturedCount: Int = 0,
    val expandedCardIds: Set<Long> = emptySet(),
    val recentlyCleared: List<NotificationRecord> = emptyList(),
    val snackbarMessage: String? = null,
    val isSetupCompleted: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val selectedAppFilter: String? = null,
    val isAppFilterSheetVisible: Boolean = false,
    val appFilterSearchQuery: String = "",
    val availableAppFilters: List<AppFilterItem> = emptyList(),
    val filteredNotifications: List<NotificationRecord> = emptyList()
)
