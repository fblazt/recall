package com.notificationhistory

import com.notificationhistory.ui.components.formatNotificationSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStatusHeaderTest {

    @Test
    fun formatNotificationSummary_withZero_returnsPluralText() {
        assertEquals("0 notifications • Last 3 days", formatNotificationSummary(0))
    }

    @Test
    fun formatNotificationSummary_withOne_returnsSingularText() {
        assertEquals("1 notification • Last 3 days", formatNotificationSummary(1))
    }

    @Test
    fun formatNotificationSummary_withMultiple_returnsPluralText() {
        assertEquals("2 notifications • Last 3 days", formatNotificationSummary(2))
        assertEquals("42 notifications • Last 3 days", formatNotificationSummary(42))
    }

    @Test
    fun formatNotificationSummary_withLargeCount_returnsFormattedText() {
        assertEquals("1000 notifications • Last 3 days", formatNotificationSummary(1000))
    }
}
