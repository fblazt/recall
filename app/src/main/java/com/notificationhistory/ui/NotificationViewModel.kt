package com.notificationhistory.ui

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.notificationhistory.data.local.AppDatabase
import com.notificationhistory.data.repository.NotificationRepository
import com.notificationhistory.data.repository.NotificationRepositoryImpl
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.domain.model.NotificationRecord
import com.notificationhistory.service.NotificationCaptureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val application: Application,
    private val repository: NotificationRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        NotificationUiState(
            listenerState = computeListenerState(
                isServiceEnabled = isNotificationServiceEnabled(),
                isConnected = NotificationCaptureService.isConnected.value,
                hasNotifications = false
            )
        )
    )
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeNotifications(),
                NotificationCaptureService.isConnected
            ) { notifications, isConnected ->
                notifications to isConnected
            }.collect { (notifications, isConnected) ->
                _uiState.update { current ->
                    val isEnabled = isNotificationServiceEnabled()
                    current.copy(
                        notifications = notifications.map { record ->
                            record.copy(isExpanded = record.id in current.expandedCardIds)
                        },
                        capturedCount = notifications.size,
                        listenerState = computeListenerState(
                            isServiceEnabled = isEnabled,
                            isConnected = isConnected,
                            hasNotifications = notifications.isNotEmpty()
                        )
                    )
                }
            }
        }
    }

    fun isNotificationServiceEnabled(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(application)
        if (application.packageName in enabledPackages) {
            return true
        }

        val flat = Settings.Secure.getString(
            application.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val myPackage = application.packageName
        return flat.split(":").any { componentString ->
            val component = ComponentName.unflattenFromString(componentString)
            component?.packageName == myPackage
        }
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

    fun toggleCardExpansion(id: Long) {
        _uiState.update { current ->
            val newExpanded = if (id in current.expandedCardIds) {
                current.expandedCardIds - id
            } else {
                current.expandedCardIds + id
            }
            current.copy(
                expandedCardIds = newExpanded,
                notifications = current.notifications.map { record ->
                    record.copy(isExpanded = record.id in newExpanded)
                }
            )
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            val cleared = repository.clearAllNotifications()
            _uiState.update { current ->
                current.copy(
                    recentlyCleared = cleared,
                    snackbarMessage = "All notifications cleared"
                )
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
                current.copy(expandedCardIds = current.expandedCardIds - id)
            }
        }
    }

    fun refreshState() {
        val isEnabled = isNotificationServiceEnabled()
        val isConnected = NotificationCaptureService.isConnected.value
        _uiState.update { current ->
            current.copy(
                listenerState = computeListenerState(
                    isServiceEnabled = isEnabled,
                    isConnected = isConnected,
                    hasNotifications = current.notifications.isNotEmpty()
                )
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
