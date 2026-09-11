package com.notificationhistory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationhistory.data.local.AppDatabase
import com.notificationhistory.data.local.UserPreferences
import com.notificationhistory.data.repository.NotificationRepository
import com.notificationhistory.data.repository.NotificationRepositoryImpl
import com.notificationhistory.domain.model.AppFilterItem
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.domain.model.NotificationRecord
import com.notificationhistory.service.NotificationCaptureService
import com.notificationhistory.util.SettingsNavigationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val application: Application,
    private val repository: NotificationRepository,
    private val userPreferences: UserPreferences = UserPreferences(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        run {
            val isEnabled = isNotificationServiceEnabled()
            if (isEnabled && !userPreferences.isSetupCompleted()) {
                userPreferences.setSetupCompleted(true)
            }
            NotificationUiState(
                listenerState = computeListenerState(
                    isServiceEnabled = isEnabled,
                    isConnected = NotificationCaptureService.isConnected.value,
                    hasNotifications = false
                ),
                isSetupCompleted = userPreferences.isSetupCompleted() || isEnabled
            )
        }
    )
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        val initialEnabled = isNotificationServiceEnabled()
        if (initialEnabled && !userPreferences.isSetupCompleted()) {
            userPreferences.setSetupCompleted(true)
        }

        viewModelScope.launch {
            combine(
                repository.observeNotifications(),
                NotificationCaptureService.isConnected,
                userPreferences.observeSetupCompleted()
            ) { notifications, isConnected, isSetupCompleted ->
                Triple(notifications, isConnected, isSetupCompleted)
            }.collect { (notifications, isConnected, isSetupCompleted) ->
                val isEnabled = isNotificationServiceEnabled()
                if (isEnabled && !isSetupCompleted) {
                    userPreferences.setSetupCompleted(true)
                }
                _uiState.update { current ->
                    val base = current.copy(
                        capturedCount = notifications.size,
                        listenerState = computeListenerState(
                            isServiceEnabled = isEnabled,
                            isConnected = isConnected,
                            hasNotifications = notifications.isNotEmpty()
                        ),
                        isSetupCompleted = isSetupCompleted || isEnabled
                    )
                    base.updateDerivedState(newNotifications = notifications)
                }
            }
        }
    }

    fun isNotificationServiceEnabled(): Boolean {
        return SettingsNavigationHelper.isNotificationListenerEnabled(getApplication())
    }

    internal fun computeListenerState(
        isServiceEnabled: Boolean,
        isConnected: Boolean,
        hasNotifications: Boolean
    ): ListenerState {
        return when {
            isServiceEnabled && isConnected -> ListenerState.Active
            !isServiceEnabled && hasNotifications -> ListenerState.Paused
            !isServiceEnabled && !hasNotifications -> ListenerState.Inactive
            hasNotifications -> ListenerState.Paused
            else -> ListenerState.Inactive
        }
    }

    internal fun computeFilteredNotifications(
        notifications: List<NotificationRecord>,
        searchQuery: String,
        selectedAppFilter: String?,
        expandedCardIds: Set<Long>
    ): List<NotificationRecord> {
        val hasFilter = selectedAppFilter != null
        val hasQuery = searchQuery.isNotBlank()
        val query = searchQuery.trim()

        return notifications.filter { record ->
            val matchesApp = !hasFilter || record.packageName == selectedAppFilter
            val matchesQuery = !hasQuery || (
                record.title.contains(query, ignoreCase = true) ||
                record.text.contains(query, ignoreCase = true) ||
                record.appName.contains(query, ignoreCase = true)
            )
            matchesApp && matchesQuery
        }.map { record ->
            record.copy(isExpanded = record.id in expandedCardIds)
        }
    }

    internal fun computeAvailableAppFilters(
        notifications: List<NotificationRecord>,
        searchQuery: String,
        selectedAppFilter: String?
    ): List<AppFilterItem> {
        val hasQuery = searchQuery.isNotBlank()
        val query = searchQuery.trim()

        return notifications
            .groupBy { it.packageName }
            .map { (packageName, records) ->
                val appName = records.firstOrNull { it.appName.isNotBlank() }?.appName
                    ?: records.firstOrNull()?.appName
                    ?: packageName
                val notificationCount = records.size
                val matchCount = if (hasQuery) {
                    records.count { record ->
                        record.title.contains(query, ignoreCase = true) ||
                        record.text.contains(query, ignoreCase = true) ||
                        record.appName.contains(query, ignoreCase = true)
                    }
                } else {
                    notificationCount
                }
                val isSelected = packageName == selectedAppFilter

                AppFilterItem(
                    packageName = packageName,
                    appName = appName,
                    notificationCount = notificationCount,
                    matchCount = matchCount,
                    isSelected = isSelected
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    private fun NotificationUiState.updateDerivedState(
        newNotifications: List<NotificationRecord> = this.notifications,
        newSearchQuery: String = this.searchQuery,
        newSelectedAppFilter: String? = this.selectedAppFilter,
        newExpandedCardIds: Set<Long> = this.expandedCardIds
    ): NotificationUiState {
        val mappedNotifications = newNotifications.map { record ->
            record.copy(isExpanded = record.id in newExpandedCardIds)
        }
        val filtered = computeFilteredNotifications(
            notifications = mappedNotifications,
            searchQuery = newSearchQuery,
            selectedAppFilter = newSelectedAppFilter,
            expandedCardIds = newExpandedCardIds
        )
        val available = computeAvailableAppFilters(
            notifications = mappedNotifications,
            searchQuery = newSearchQuery,
            selectedAppFilter = newSelectedAppFilter
        )
        return this.copy(
            notifications = mappedNotifications,
            filteredNotifications = filtered,
            availableAppFilters = available,
            searchQuery = newSearchQuery,
            selectedAppFilter = newSelectedAppFilter,
            expandedCardIds = newExpandedCardIds
        )
    }

    fun toggleCardExpansion(id: Long) {
        _uiState.update { current ->
            val newExpanded = if (id in current.expandedCardIds) {
                current.expandedCardIds - id
            } else {
                current.expandedCardIds + id
            }
            current.updateDerivedState(newExpandedCardIds = newExpanded)
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { current ->
            val newSearchQuery = if (!active) "" else current.searchQuery
            current.copy(isSearchActive = active)
                .updateDerivedState(newSearchQuery = newSearchQuery)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.updateDerivedState(newSearchQuery = query)
        }
    }

    fun clearSearchQuery() {
        onSearchQueryChanged("")
    }

    fun setAppFilterSheetVisible(visible: Boolean) {
        _uiState.update { current ->
            current.copy(
                isAppFilterSheetVisible = visible,
                appFilterSearchQuery = if (!visible) "" else current.appFilterSearchQuery
            )
        }
    }

    fun onAppFilterSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(appFilterSearchQuery = query)
        }
    }

    fun selectAppFilter(packageName: String?) {
        _uiState.update { current ->
            current.updateDerivedState(newSelectedAppFilter = packageName)
        }
    }

    fun clearAppFilter() {
        selectAppFilter(null)
    }

    fun clearAll() {
        viewModelScope.launch {
            val cleared = repository.clearAllNotifications()
            _uiState.update { current ->
                current.copy(
                    recentlyCleared = cleared,
                    snackbarMessage = "All notifications cleared"
                ).updateDerivedState(newNotifications = emptyList())
            }
        }
    }

    fun undoClear() {
        viewModelScope.launch {
            val toRestore = _uiState.value.recentlyCleared
            if (toRestore.isNotEmpty()) {
                repository.restoreNotifications(toRestore)
                _uiState.update { current ->
                    current.copy(recentlyCleared = emptyList())
                }
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { current ->
            current.copy(snackbarMessage = null)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotificationById(id)
            _uiState.update { current ->
                val newExpanded = current.expandedCardIds - id
                val newNotifications = current.notifications.filterNot { it.id == id }
                current.copy(capturedCount = newNotifications.size)
                    .updateDerivedState(
                        newNotifications = newNotifications,
                        newExpandedCardIds = newExpanded
                    )
            }
        }
    }

    fun markSetupCompleted(completed: Boolean = true) {
        userPreferences.setSetupCompleted(completed)
        _uiState.update { current ->
            current.copy(isSetupCompleted = completed)
        }
    }

    fun refreshState() {
        val isEnabled = isNotificationServiceEnabled()
        if (isEnabled && !userPreferences.isSetupCompleted()) {
            userPreferences.setSetupCompleted(true)
        }
        val isConnected = NotificationCaptureService.isConnected.value
        val isSetupCompleted = userPreferences.isSetupCompleted()
        _uiState.update { current ->
            current.copy(
                listenerState = computeListenerState(
                    isServiceEnabled = isEnabled,
                    isConnected = isConnected,
                    hasNotifications = current.notifications.isNotEmpty()
                ),
                isSetupCompleted = isSetupCompleted || isEnabled
            )
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                    val database = AppDatabase.getDatabase(application)
                    val repository = NotificationRepositoryImpl(database.notificationDao())
                    return NotificationViewModel(application, repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
