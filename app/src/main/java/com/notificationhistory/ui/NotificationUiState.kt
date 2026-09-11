package com.notificationhistory.ui

import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.domain.model.NotificationRecord

data class NotificationUiState(
    val notifications: List<NotificationRecord> = emptyList(),
    val listenerState: ListenerState = ListenerState.Inactive,
    val capturedCount: Int = 0,
    val expandedCardIds: Set<Long> = emptySet(),
    val recentlyCleared: List<NotificationRecord> = emptyList(),
    val snackbarMessage: String? = null,
    val isSetupCompleted: Boolean = false
)
