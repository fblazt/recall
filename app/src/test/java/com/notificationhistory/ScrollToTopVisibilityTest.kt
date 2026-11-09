package com.notificationhistory

import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.ui.shouldShowScrollToTop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollToTopVisibilityTest {

    @Test
    fun shouldShowScrollToTop_whenListenerInactive_returnsFalse() {
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Inactive,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 5
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Inactive,
                isSearchActive = true,
                hasNotifications = false,
                firstVisibleItemIndex = 0
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenSearchIsActive_returnsFalse() {
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = true,
                hasNotifications = true,
                firstVisibleItemIndex = 10
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenHasNoNotifications_returnsFalse() {
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = false,
                firstVisibleItemIndex = 5
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenScrolledAtOrBelowThreshold_returnsFalse() {
        // Threshold is > 2, so 0, 1, 2 must all return false
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 0
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 1
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 2
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenScrolledPastThresholdWithActiveListenerAndNotifications_returnsTrue() {
        // Boundary case: 3 is the first index > 2
        assertTrue(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 3
            )
        )
        assertTrue(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 25
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenListenerPaused_returnsTrueIfScrolled() {
        // Paused state still displays notifications and isn't Inactive
        assertTrue(
            shouldShowScrollToTop(
                listenerState = ListenerState.Paused,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 4
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenListenerPaused_returnsFalseIfGuardsFail() {
        // Paused state must still respect search, notification count, and scroll threshold
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Paused,
                isSearchActive = true,
                hasNotifications = true,
                firstVisibleItemIndex = 4
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Paused,
                isSearchActive = false,
                hasNotifications = false,
                firstVisibleItemIndex = 4
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Paused,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = 2
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenLargeIndex_returnsTrue() {
        assertTrue(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = Int.MAX_VALUE
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_withBooleanThresholdOverload_behavesIdentically() {
        // Overload used directly in NotificationHistoryScreen
        assertTrue(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                isScrolledPastThreshold = true
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                isScrolledPastThreshold = false
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Inactive,
                isSearchActive = false,
                hasNotifications = true,
                isScrolledPastThreshold = true
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = true,
                hasNotifications = true,
                isScrolledPastThreshold = true
            )
        )
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = false,
                isScrolledPastThreshold = true
            )
        )
    }

    @Test
    fun shouldShowScrollToTop_whenNegativeIndex_returnsFalse() {
        assertFalse(
            shouldShowScrollToTop(
                listenerState = ListenerState.Active,
                isSearchActive = false,
                hasNotifications = true,
                firstVisibleItemIndex = -1
            )
        )
    }
}
