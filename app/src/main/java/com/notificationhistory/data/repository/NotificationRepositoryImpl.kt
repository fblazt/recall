package com.notificationhistory.data.repository

import com.notificationhistory.data.local.NotificationDao
import com.notificationhistory.data.local.toDomainModel
import com.notificationhistory.data.local.toEntity
import com.notificationhistory.domain.model.NotificationRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NotificationRepositoryImpl(
    private val dao: NotificationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<NotificationRecord>> =
        dao.getAllNotifications()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(ioDispatcher)

    override fun observeNotificationCount(): Flow<Int> =
        dao.getNotificationCount()
            .flowOn(ioDispatcher)

    override suspend fun insertNotification(record: NotificationRecord): Long =
        withContext(ioDispatcher) {
            dao.insert(record.toEntity())
        }

    override suspend fun insertNotifications(records: List<NotificationRecord>) =
        withContext(ioDispatcher) {
            dao.insertAll(records.map { it.toEntity() })
        }

    override suspend fun purgeOldNotifications(maxAgeMillis: Long): Int =
        withContext(ioDispatcher) {
            val cutoff = System.currentTimeMillis() - maxAgeMillis
            dao.deleteOlderThan(cutoff)
        }

    override suspend fun clearAllNotifications(): List<NotificationRecord> =
        withContext(ioDispatcher) {
            val snapshot = dao.getAllNotifications().first().map { it.toDomainModel() }
            dao.deleteAll()
            snapshot
        }

    override suspend fun restoreNotifications(records: List<NotificationRecord>) =
        withContext(ioDispatcher) {
            dao.insertAll(records.map { it.toEntity() })
        }

    override suspend fun deleteNotificationById(id: Long) =
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }
}
