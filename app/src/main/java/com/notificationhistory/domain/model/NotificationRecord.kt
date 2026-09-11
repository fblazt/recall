package com.notificationhistory.domain.model

data class NotificationRecord(
    val id: Long = 0L,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postTime: Long,
    val notificationKey: String,
    val category: String? = null,
    val isExpanded: Boolean = false
)
