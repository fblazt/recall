package com.notificationhistory

import com.notificationhistory.data.local.NotificationDao
import com.notificationhistory.data.local.NotificationEntity
import com.notificationhistory.data.repository.NotificationRepositoryImpl
import com.notificationhistory.domain.model.NotificationRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * In-memory test double for [NotificationDao].
 */
class FakeNotificationDao : NotificationDao {
    private val notificationsFlow = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(notification: NotificationEntity): Long {
        val id = if (notification.id == 0L) {
            nextId++
        } else {
            if (notification.id >= nextId) {
                nextId = notification.id + 1
            }
            notification.id
        }
        val entity = notification.copy(id = id)
        val current = notificationsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        current.add(entity)
        current.sortByDescending { it.postTime }
        notificationsFlow.value = current
        return id
    }

    override suspend fun insertAll(notifications: List<NotificationEntity>) {
        val current = notificationsFlow.value.toMutableList()
        for (notification in notifications) {
            val id = if (notification.id == 0L) {
                nextId++
            } else {
                if (notification.id >= nextId) {
                    nextId = notification.id + 1
                }
                notification.id
            }
            val entity = notification.copy(id = id)
            current.removeAll { it.id == id }
            current.add(entity)
        }
        current.sortByDescending { it.postTime }
        notificationsFlow.value = current
    }

    override fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationsFlow

    override fun getNotificationCount(): Flow<Int> = notificationsFlow.map { it.size }

    override suspend fun deleteOlderThan(cutoffTimestamp: Long): Int {
        val current = notificationsFlow.value
        val toKeep = current.filter { it.postTime >= cutoffTimestamp }
        val deletedCount = current.size - toKeep.size
        notificationsFlow.value = toKeep
        return deletedCount
    }

    override suspend fun deleteAll(): Int {
        val count = notificationsFlow.value.size
        notificationsFlow.value = emptyList()
        return count
    }

    override suspend fun deleteById(id: Long) {
        val current = notificationsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        notificationsFlow.value = current
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeNotificationDao
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeNotificationDao()
        repository = NotificationRepositoryImpl(fakeDao, testDispatcher)
    }

    @Test
    fun insertAndObserveNotifications() = runTest(testDispatcher) {
        val record = NotificationRecord(
            packageName = "com.example.app",
            appName = "Example App",
            title = "Test Notification Title",
            text = "Test Notification Content",
            postTime = 1700000000000L,
            notificationKey = "key_test_1",
            category = "msg",
            isExpanded = false
        )

        val insertedId = repository.insertNotification(record)
        val notifications = repository.observeNotifications().first()

        assertEquals(1, notifications.size)
        val retrieved = notifications[0]
        assertEquals(insertedId, retrieved.id)
        assertEquals(record.packageName, retrieved.packageName)
        assertEquals(record.appName, retrieved.appName)
        assertEquals(record.title, retrieved.title)
        assertEquals(record.text, retrieved.text)
        assertEquals(record.postTime, retrieved.postTime)
        assertEquals(record.notificationKey, retrieved.notificationKey)
        assertEquals(record.category, retrieved.category)
        assertEquals(false, retrieved.isExpanded)
    }

    @Test
    fun purgeOldNotifications_removesNotificationsOlderThan72Hours() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val oneHourMillis = 1L * 60 * 60 * 1000L
        val seventyFiveHoursMillis = 75L * 60 * 60 * 1000L
        val seventyTwoHoursMillis = 72L * 60 * 60 * 1000L

        val recentRecord = NotificationRecord(
            packageName = "com.example.recent",
            appName = "Recent App",
            title = "Recent Title",
            text = "Recent Notification",
            postTime = now - oneHourMillis,
            notificationKey = "key_recent"
        )
        val oldRecord = NotificationRecord(
            packageName = "com.example.old",
            appName = "Old App",
            title = "Old Title",
            text = "Old Notification",
            postTime = now - seventyFiveHoursMillis,
            notificationKey = "key_old"
        )

        repository.insertNotification(recentRecord)
        repository.insertNotification(oldRecord)

        val deletedCount = repository.purgeOldNotifications(seventyTwoHoursMillis)
        assertEquals(1, deletedCount)

        val remaining = repository.observeNotifications().first()
        assertEquals(1, remaining.size)
        assertEquals("com.example.recent", remaining[0].packageName)
    }

    @Test
    fun clearAllAndUndoRestore() = runTest(testDispatcher) {
        val record1 = NotificationRecord(
            packageName = "com.example.app1",
            appName = "App One",
            title = "Title 1",
            text = "Message 1",
            postTime = 1000L,
            notificationKey = "key_1"
        )
        val record2 = NotificationRecord(
            packageName = "com.example.app2",
            appName = "App Two",
            title = "Title 2",
            text = "Message 2",
            postTime = 2000L,
            notificationKey = "key_2"
        )

        repository.insertNotifications(listOf(record1, record2))

        val initialItems = repository.observeNotifications().first()
        assertEquals(2, initialItems.size)

        // Clear all and assert returned list matches snapshot
        val clearedItems = repository.clearAllNotifications()
        assertEquals(2, clearedItems.size)

        // Verify database is now empty
        val afterClear = repository.observeNotifications().first()
        assertTrue(afterClear.isEmpty())

        // Restore cleared items
        repository.restoreNotifications(clearedItems)

        val afterRestore = repository.observeNotifications().first()
        assertEquals(2, afterRestore.size)
        assertEquals(clearedItems, afterRestore)
    }

    @Test
    fun deleteNotificationById_removesSpecificNotification() = runTest(testDispatcher) {
        val record1 = NotificationRecord(
            packageName = "com.example.app1",
            appName = "App One",
            title = "Title 1",
            text = "Message 1",
            postTime = 1000L,
            notificationKey = "key_1"
        )
        val record2 = NotificationRecord(
            packageName = "com.example.app2",
            appName = "App Two",
            title = "Title 2",
            text = "Message 2",
            postTime = 2000L,
            notificationKey = "key_2"
        )

        val id1 = repository.insertNotification(record1)
        val id2 = repository.insertNotification(record2)

        repository.deleteNotificationById(id1)

        val remaining = repository.observeNotifications().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)
    }

    @Test
    fun observeNotificationCount_reflectsDatabaseSize() = runTest(testDispatcher) {
        assertEquals(0, repository.observeNotificationCount().first())

        val record = NotificationRecord(
            packageName = "com.example.app",
            appName = "App",
            title = "Title",
            text = "Message",
            postTime = 1000L,
            notificationKey = "key"
        )
        repository.insertNotification(record)
        assertEquals(1, repository.observeNotificationCount().first())
    }
}
