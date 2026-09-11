package com.notificationhistory.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.notificationhistory.domain.model.NotificationRecord

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["postTime"]),
        Index(value = ["packageName"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val notificationKey: String,
    val category: String? = null
)

fun NotificationEntity.toDomainModel(): NotificationRecord = NotificationRecord(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    postTime = postTime,
    notificationKey = notificationKey,
    category = category,
    isExpanded = false
)

fun NotificationEntity.toRecord(): NotificationRecord = toDomainModel()

fun NotificationRecord.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    packageName = packageName,
    appName = appName,
    title = title,
    text = text,
    postTime = postTime,
    notificationKey = notificationKey,
    category = category
)
