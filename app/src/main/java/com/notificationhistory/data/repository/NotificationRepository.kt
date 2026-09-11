package com.notificationhistory.data.repository

import com.notificationhistory.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<NotificationRecord>>
    fun observeNotificationCount(): Flow<Int>
    suspend fun insertNotification(record: NotificationRecord): Long
    suspend fun insertNotifications(records: List<NotificationRecord>)
    suspend fun purgeOldNotifications(maxAgeMillis: Long = 72L * 60 * 60 * 1000L): Int
    suspend fun clearAllNotifications(): List<NotificationRecord>
    suspend fun restoreNotifications(records: List<NotificationRecord>)
    suspend fun deleteNotificationById(id: Long)
}
