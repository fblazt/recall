package com.notificationhistory.domain.model

data class AppFilterItem(
    val packageName: String,
    val appName: String,
    val notificationCount: Int = 0,
    val matchCount: Int = 0,
    val isSelected: Boolean = false
)
