package com.notificationhistory.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notificationhistory.data.local.AppDatabase
import com.notificationhistory.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val sbnPackageName = sbn.packageName ?: return
        if (sbnPackageName.isBlank() || sbnPackageName == applicationContext.packageName) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)
        val title = rawTitle?.toString()?.trim().orEmpty()

        val rawText = extras?.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.filterNotNull()?.joinToString("\n")
        val text = rawText?.toString()?.trim().orEmpty()

        if (title.isBlank() && text.isBlank()) {
            return
        }

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(
                sbnPackageName,
                PackageManager.ApplicationInfoFlags.of(0L)
            )
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbnPackageName
        }.ifBlank { sbnPackageName }

        val postTime = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis()
        val notificationKey = sbn.key ?: ""
        val category = notification.category

        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val dao = database.notificationDao()
                val entity = NotificationEntity(
                    packageName = sbnPackageName,
                    appName = appName,
                    title = title,
                    text = text,
                    postTime = postTime,
                    notificationKey = notificationKey,
                    category = category
                )
                dao.insert(entity)
                val cutoffTimestamp = System.currentTimeMillis() - RETENTION_WINDOW_MILLIS
                dao.deleteOlderThan(cutoffTimestamp)
            } catch (_: Exception) {
                // Ignore capture/cleanup errors gracefully
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isConnected.value = false
        serviceScope.cancel()
    }

    companion object {
        private const val RETENTION_WINDOW_MILLIS = 72L * 60 * 60 * 1000L

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }
}
