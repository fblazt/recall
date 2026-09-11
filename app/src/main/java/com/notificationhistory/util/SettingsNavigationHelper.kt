package com.notificationhistory.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.notificationhistory.service.NotificationCaptureService

object SettingsNavigationHelper {

    private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

    fun openNotificationListenerSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val componentName = ComponentName(context, NotificationCaptureService::class.java)
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Fallback to general notification listener settings
            }
        }

        try {
            val fallbackIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            // In case no activity can handle it
        }
    }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (context.packageName in enabledPackages) {
                return true
            }
        } catch (e: Exception) {
            // Fallback to Settings.Secure
        }

        val flat = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        ) ?: return false

        val myPackage = context.packageName
        return flat.split(":").any { componentString ->
            val component = ComponentName.unflattenFromString(componentString)
            component?.packageName == myPackage
        }
    }
}
