package com.notificationhistory

import com.notificationhistory.domain.model.AppFilterItem
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.domain.model.NotificationRecord
import com.notificationhistory.ui.NotificationUiState
import com.notificationhistory.ui.NotificationViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sun.misc.Unsafe

class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel

    private val sampleNotifications = listOf(
        NotificationRecord(
            id = 1L,
            packageName = "com.slack",
            appName = "Slack",
            title = "Engineering Standup",
            text = "Meeting in 10 minutes",
            postTime = 1000L,
            notificationKey = "key_1"
        ),
        NotificationRecord(
            id = 2L,
            packageName = "com.slack",
            appName = "Slack",
            title = "Deployment Status",
            text = "Release v2.0 was successful",
            postTime = 2000L,
            notificationKey = "key_2"
        ),
        NotificationRecord(
            id = 3L,
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "Alice",
            text = "Are you available for engineering review?",
            postTime = 3000L,
            notificationKey = "key_3"
        ),
        NotificationRecord(
            id = 4L,
            packageName = "com.google.android.gm",
            appName = "Gmail",
            title = "Weekly Newsletter",
            text = "Check out the new developer docs",
            postTime = 4000L,
            notificationKey = "key_4"
        ),
        NotificationRecord(
            id = 5L,
            packageName = "com.twitter.android",
            appName = "X",
            title = "Trending",
            text = "Tech news today",
            postTime = 5000L,
            notificationKey = "key_5"
        )
    )

    private fun createTestViewModel(initialState: NotificationUiState = NotificationUiState()): NotificationViewModel {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as Unsafe
        val vm = unsafe.allocateInstance(NotificationViewModel::class.java) as NotificationViewModel

        val stateFlow = MutableStateFlow(initialState)
        val uiStateField = NotificationViewModel::class.java.getDeclaredField("_uiState").apply { isAccessible = true }
        uiStateField.set(vm, stateFlow)

        val publicUiStateField = NotificationViewModel::class.java.getDeclaredField("uiState").apply { isAccessible = true }
        publicUiStateField.set(vm, stateFlow.asStateFlow())

        return vm
    }

    private fun createViewModelWithNotifications(
        notifications: List<NotificationRecord> = sampleNotifications
    ): NotificationViewModel {
        val availableFilters = viewModel.computeAvailableAppFilters(
            notifications = notifications,
            searchQuery = "",
            selectedAppFilter = null
        )
        val baseState = NotificationUiState(
            notifications = notifications,
            capturedCount = notifications.size,
            filteredNotifications = notifications,
            availableAppFilters = availableFilters
        )
        return createTestViewModel(baseState)
    }

    @Before
    fun setUp() {
        viewModel = createTestViewModel()
    }

    // =========================================================================
    // 1. computeFilteredNotifications Unit Tests
    // =========================================================================

    @Test
    fun computeFilteredNotifications_whenQueryIsEmptyAndNoAppFilter_returnsAllNotifications() {
        val result = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )

        assertEquals(5, result.size)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result.map { it.id })
    }

    @Test
    fun computeFilteredNotifications_whenQueryIsWhitespace_treatedAsEmptyAndReturnsAll() {
        val result = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "    ",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )

        assertEquals(5, result.size)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result.map { it.id })
    }

    @Test
    fun computeFilteredNotifications_filtersCaseInsensitively_byTitle() {
        // Upper case query matching lowercase in title
        val upperResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "STANDUP",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, upperResult.size)
        assertEquals(1L, upperResult.first().id)

        // Lower case query matching capitalized word in title
        val lowerResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "weekly",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, lowerResult.size)
        assertEquals(4L, lowerResult.first().id)

        // Mixed case query matching title substring
        val mixedResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "DePlOyMeNt",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, mixedResult.size)
        assertEquals(2L, mixedResult.first().id)
    }

    @Test
    fun computeFilteredNotifications_filtersCaseInsensitively_byText() {
        // Matching text "Meeting in 10 minutes"
        val result1 = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "minutes",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, result1.size)
        assertEquals(1L, result1.first().id)

        // Matching text "Release v2.0 was successful" with uppercase
        val result2 = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "RELEASE V2.0",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, result2.size)
        assertEquals(2L, result2.first().id)

        // Matching text "Are you available for engineering review?"
        val result3 = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "available",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, result3.size)
        assertEquals(3L, result3.first().id)
    }

    @Test
    fun computeFilteredNotifications_filtersCaseInsensitively_byAppName() {
        // Query matching app name "Slack" (matches records 1 and 2)
        val slackResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "slack",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(2, slackResult.size)
        assertEquals(listOf(1L, 2L), slackResult.map { it.id })

        // Query matching app name "WhatsApp" with uppercase
        val whatsappResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "WHATSAPP",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, whatsappResult.size)
        assertEquals(3L, whatsappResult.first().id)

        // Query matching app name "Gmail" with mixed case
        val gmailResult = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "gMaIl",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, gmailResult.size)
        assertEquals(4L, gmailResult.first().id)
    }

    @Test
    fun computeFilteredNotifications_filtersBySelectedAppFilter() {
        val slackFiltered = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = "com.slack",
            expandedCardIds = emptySet()
        )
        assertEquals(2, slackFiltered.size)
        assertTrue(slackFiltered.all { it.packageName == "com.slack" })

        val whatsappFiltered = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = "com.whatsapp",
            expandedCardIds = emptySet()
        )
        assertEquals(1, whatsappFiltered.size)
        assertEquals("com.whatsapp", whatsappFiltered.first().packageName)

        val gmailFiltered = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = "com.google.android.gm",
            expandedCardIds = emptySet()
        )
        assertEquals(1, gmailFiltered.size)
        assertEquals("com.google.android.gm", gmailFiltered.first().packageName)
    }

    @Test
    fun computeFilteredNotifications_combinesQueryAndPackageFilterTogether() {
        // "engineering" appears in Slack (#1 title) and WhatsApp (#3 text)
        // With package filter = com.slack, only #1 should match
        val slackEngineering = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "engineering",
            selectedAppFilter = "com.slack",
            expandedCardIds = emptySet()
        )
        assertEquals(1, slackEngineering.size)
        assertEquals(1L, slackEngineering.first().id)
        assertEquals("com.slack", slackEngineering.first().packageName)

        // With package filter = com.whatsapp, only #3 should match
        val whatsappEngineering = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "engineering",
            selectedAppFilter = "com.whatsapp",
            expandedCardIds = emptySet()
        )
        assertEquals(1, whatsappEngineering.size)
        assertEquals(3L, whatsappEngineering.first().id)
        assertEquals("com.whatsapp", whatsappEngineering.first().packageName)

        // Matching Slack with query "Release"
        val slackRelease = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "Release",
            selectedAppFilter = "com.slack",
            expandedCardIds = emptySet()
        )
        assertEquals(1, slackRelease.size)
        assertEquals(2L, slackRelease.first().id)
    }

    @Test
    fun computeFilteredNotifications_whenNoNotificationsMatch_returnsEmptyList() {
        // Query that matches nothing
        val noQueryMatch = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "non_existent_token_xyz",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertTrue(noQueryMatch.isEmpty())

        // Non-existent package filter
        val noPackageMatch = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = "com.nonexistent.app",
            expandedCardIds = emptySet()
        )
        assertTrue(noPackageMatch.isEmpty())

        // Package matches but query does not match for that package
        val combinedNoMatch = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "Alice",
            selectedAppFilter = "com.slack",
            expandedCardIds = emptySet()
        )
        assertTrue(combinedNoMatch.isEmpty())
    }

    @Test
    fun computeFilteredNotifications_trimsLeadingAndTrailingWhitespaceFromQuery() {
        val result = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "   Standup   ",
            selectedAppFilter = null,
            expandedCardIds = emptySet()
        )
        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun computeFilteredNotifications_preservesAndSetsCardExpansionStatus() {
        val expandedIds = setOf(2L, 4L)
        val result = viewModel.computeFilteredNotifications(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = null,
            expandedCardIds = expandedIds
        )

        assertEquals(5, result.size)
        assertFalse(result.first { it.id == 1L }.isExpanded)
        assertTrue(result.first { it.id == 2L }.isExpanded)
        assertFalse(result.first { it.id == 3L }.isExpanded)
        assertTrue(result.first { it.id == 4L }.isExpanded)
        assertFalse(result.first { it.id == 5L }.isExpanded)
    }

    // =========================================================================
    // 2. computeAvailableAppFilters Unit Tests
    // =========================================================================

    @Test
    fun computeAvailableAppFilters_computesCorrectNotificationCountPerAppPackage() {
        val filters = viewModel.computeAvailableAppFilters(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = null
        )

        // 4 distinct packages: Gmail (1), Slack (2), WhatsApp (1), X (1)
        assertEquals(4, filters.size)

        val slack = filters.first { it.packageName == "com.slack" }
        assertEquals(2, slack.notificationCount)
        assertEquals(2, slack.matchCount)

        val whatsapp = filters.first { it.packageName == "com.whatsapp" }
        assertEquals(1, whatsapp.notificationCount)
        assertEquals(1, whatsapp.matchCount)

        val gmail = filters.first { it.packageName == "com.google.android.gm" }
        assertEquals(1, gmail.notificationCount)
        assertEquals(1, gmail.matchCount)

        val x = filters.first { it.packageName == "com.twitter.android" }
        assertEquals(1, x.notificationCount)
        assertEquals(1, x.matchCount)
    }

    @Test
    fun computeAvailableAppFilters_computesCorrectMatchCountPerAppPackage_whenQueryProvided() {
        // Query "engineering":
        // - Slack has 2 notifications, 1 matches ("Engineering Standup")
        // - WhatsApp has 1 notification, 1 matches ("Are you available for engineering review?")
        // - Gmail has 1 notification, 0 matches
        // - X has 1 notification, 0 matches
        val filters = viewModel.computeAvailableAppFilters(
            notifications = sampleNotifications,
            searchQuery = "engineering",
            selectedAppFilter = null
        )

        val slack = filters.first { it.packageName == "com.slack" }
        assertEquals(2, slack.notificationCount)
        assertEquals(1, slack.matchCount)

        val whatsapp = filters.first { it.packageName == "com.whatsapp" }
        assertEquals(1, whatsapp.notificationCount)
        assertEquals(1, whatsapp.matchCount)

        val gmail = filters.first { it.packageName == "com.google.android.gm" }
        assertEquals(1, gmail.notificationCount)
        assertEquals(0, gmail.matchCount)

        val x = filters.first { it.packageName == "com.twitter.android" }
        assertEquals(1, x.notificationCount)
        assertEquals(0, x.matchCount)
    }

    @Test
    fun computeAvailableAppFilters_setsIsSelectedTrue_forMatchingPackageOnly() {
        val filters = viewModel.computeAvailableAppFilters(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = "com.slack"
        )

        val slack = filters.first { it.packageName == "com.slack" }
        assertTrue(slack.isSelected)

        val nonSlack = filters.filter { it.packageName != "com.slack" }
        assertTrue(nonSlack.all { !it.isSelected })
    }

    @Test
    fun computeAvailableAppFilters_setsAllIsSelectedFalse_whenNoPackageSelected() {
        val filters = viewModel.computeAvailableAppFilters(
            notifications = sampleNotifications,
            searchQuery = "",
            selectedAppFilter = null
        )

        assertTrue(filters.all { !it.isSelected })
    }

    @Test
    fun computeAvailableAppFilters_sortsAlphabeticallyByAppNameCaseInsensitive() {
        val mixedNotifications = listOf(
            NotificationRecord(
                id = 10L,
                packageName = "com.zebra",
                appName = "zebra App",
                title = "Z Title",
                text = "Z Text",
                postTime = 100L,
                notificationKey = "key_10"
            ),
            NotificationRecord(
                id = 20L,
                packageName = "com.apple",
                appName = "Apple Music",
                title = "A Title",
                text = "A Text",
                postTime = 200L,
                notificationKey = "key_20"
            ),
            NotificationRecord(
                id = 30L,
                packageName = "com.banana",
                appName = "banana Notes",
                title = "B Title",
                text = "B Text",
                postTime = 300L,
                notificationKey = "key_30"
            ),
            NotificationRecord(
                id = 40L,
                packageName = "com.cat",
                appName = "Cat Feed",
                title = "C Title",
                text = "C Text",
                postTime = 400L,
                notificationKey = "key_40"
            )
        )

        val filters = viewModel.computeAvailableAppFilters(
            notifications = mixedNotifications,
            searchQuery = "",
            selectedAppFilter = null
        )

        val names = filters.map { it.appName }
        assertEquals(listOf("Apple Music", "banana Notes", "Cat Feed", "zebra App"), names)
    }

    @Test
    fun computeAvailableAppFilters_whenEmptyNotifications_returnsEmptyList() {
        val filters = viewModel.computeAvailableAppFilters(
            notifications = emptyList(),
            searchQuery = "",
            selectedAppFilter = null
        )
        assertTrue(filters.isEmpty())
    }

    @Test
    fun computeAvailableAppFilters_usesNonBlankAppName_whenSomeRecordsHaveBlankName() {
        val mixedRecords = listOf(
            NotificationRecord(
                id = 101L,
                packageName = "com.slack",
                appName = "",
                title = "Title 1",
                text = "Text 1",
                postTime = 100L,
                notificationKey = "key_101"
            ),
            NotificationRecord(
                id = 102L,
                packageName = "com.slack",
                appName = "Slack",
                title = "Title 2",
                text = "Text 2",
                postTime = 200L,
                notificationKey = "key_102"
            )
        )

        val filters = viewModel.computeAvailableAppFilters(
            notifications = mixedRecords,
            searchQuery = "",
            selectedAppFilter = null
        )

        assertEquals(1, filters.size)
        assertEquals("Slack", filters.first().appName)
    }

    // =========================================================================
    // 3. State Transition Logic on NotificationViewModel
    // =========================================================================

    @Test
    fun setSearchActive_activatingSearch_setsIsSearchActiveTrue() {
        val vm = createViewModelWithNotifications()
        assertFalse(vm.uiState.value.isSearchActive)

        vm.setSearchActive(true)

        assertTrue(vm.uiState.value.isSearchActive)
    }

    @Test
    fun setSearchActive_deactivatingSearch_resetsQueryAndRestoresFilteredNotifications() {
        val vm = createViewModelWithNotifications()
        vm.setSearchActive(true)
        vm.onSearchQueryChanged("Standup")

        assertEquals("Standup", vm.uiState.value.searchQuery)
        assertEquals(1, vm.uiState.value.filteredNotifications.size)

        vm.setSearchActive(false)

        assertFalse(vm.uiState.value.isSearchActive)
        assertEquals("", vm.uiState.value.searchQuery)
        assertEquals(5, vm.uiState.value.filteredNotifications.size)
    }

    @Test
    fun onSearchQueryChanged_updatesQueryAndFiltersNotificationsAndAppFilters() {
        val vm = createViewModelWithNotifications()

        vm.onSearchQueryChanged("engineering")

        val state = vm.uiState.value
        assertEquals("engineering", state.searchQuery)
        // Engineering matches Slack (#1) and WhatsApp (#3)
        assertEquals(2, state.filteredNotifications.size)
        assertEquals(listOf(1L, 3L), state.filteredNotifications.map { it.id })

        val slackFilter = state.availableAppFilters.first { it.packageName == "com.slack" }
        assertEquals(2, slackFilter.notificationCount)
        assertEquals(1, slackFilter.matchCount)

        val whatsappFilter = state.availableAppFilters.first { it.packageName == "com.whatsapp" }
        assertEquals(1, whatsappFilter.notificationCount)
        assertEquals(1, whatsappFilter.matchCount)

        val gmailFilter = state.availableAppFilters.first { it.packageName == "com.google.android.gm" }
        assertEquals(1, gmailFilter.notificationCount)
        assertEquals(0, gmailFilter.matchCount)
    }

    @Test
    fun clearSearchQuery_resetsQueryAndRestoresFilteredList() {
        val vm = createViewModelWithNotifications()
        vm.onSearchQueryChanged("engineering")
        assertEquals(2, vm.uiState.value.filteredNotifications.size)

        vm.clearSearchQuery()

        assertEquals("", vm.uiState.value.searchQuery)
        assertEquals(5, vm.uiState.value.filteredNotifications.size)
    }

    @Test
    fun selectAppFilter_filtersNotificationsToSelectedAppAndMarksFilterSelected() {
        val vm = createViewModelWithNotifications()

        vm.selectAppFilter("com.slack")

        val state = vm.uiState.value
        assertEquals("com.slack", state.selectedAppFilter)
        assertEquals(2, state.filteredNotifications.size)
        assertTrue(state.filteredNotifications.all { it.packageName == "com.slack" })

        val slackFilter = state.availableAppFilters.first { it.packageName == "com.slack" }
        assertTrue(slackFilter.isSelected)

        val otherFilters = state.availableAppFilters.filter { it.packageName != "com.slack" }
        assertTrue(otherFilters.all { !it.isSelected })
    }

    @Test
    fun clearAppFilter_resetsSelectedFilterAndRestoresAllNotifications() {
        val vm = createViewModelWithNotifications()
        vm.selectAppFilter("com.slack")
        assertEquals(2, vm.uiState.value.filteredNotifications.size)

        vm.clearAppFilter()

        val state = vm.uiState.value
        assertNull(state.selectedAppFilter)
        assertEquals(5, state.filteredNotifications.size)
        assertTrue(state.availableAppFilters.all { !it.isSelected })
    }

    @Test
    fun combinedSearchAndAppFilter_stateTransitions() {
        val vm = createViewModelWithNotifications()

        // 1. Select Slack filter -> only 2 Slack notifications
        vm.selectAppFilter("com.slack")
        assertEquals(2, vm.uiState.value.filteredNotifications.size)
        assertEquals("com.slack", vm.uiState.value.selectedAppFilter)

        // 2. Add search query "Standup" -> narrows to 1 notification (#1)
        vm.onSearchQueryChanged("Standup")
        assertEquals(1, vm.uiState.value.filteredNotifications.size)
        assertEquals(1L, vm.uiState.value.filteredNotifications.first().id)

        // 3. Clear query while keeping package filter -> back to 2 Slack notifications
        vm.clearSearchQuery()
        assertEquals(2, vm.uiState.value.filteredNotifications.size)
        assertEquals("com.slack", vm.uiState.value.selectedAppFilter)

        // 4. Clear package filter -> back to all 5 notifications
        vm.clearAppFilter()
        assertEquals(5, vm.uiState.value.filteredNotifications.size)
        assertNull(vm.uiState.value.selectedAppFilter)
    }

    @Test
    fun setAppFilterSheetVisible_togglesSheetAndResetsFilterSearchQueryOnClose() {
        val vm = createViewModelWithNotifications()
        assertFalse(vm.uiState.value.isAppFilterSheetVisible)

        vm.setAppFilterSheetVisible(true)
        assertTrue(vm.uiState.value.isAppFilterSheetVisible)

        vm.onAppFilterSearchQueryChanged("Slack")
        assertEquals("Slack", vm.uiState.value.appFilterSearchQuery)

        vm.setAppFilterSheetVisible(false)
        assertFalse(vm.uiState.value.isAppFilterSheetVisible)
        assertEquals("", vm.uiState.value.appFilterSearchQuery)
    }

    @Test
    fun toggleCardExpansion_togglesCardIdInExpandedSetAndNotificationItems() {
        val vm = createViewModelWithNotifications()
        assertFalse(vm.uiState.value.expandedCardIds.contains(2L))

        // Toggle card 2 on
        vm.toggleCardExpansion(2L)
        assertTrue(vm.uiState.value.expandedCardIds.contains(2L))
        assertTrue(vm.uiState.value.notifications.first { it.id == 2L }.isExpanded)
        assertTrue(vm.uiState.value.filteredNotifications.first { it.id == 2L }.isExpanded)
        assertFalse(vm.uiState.value.notifications.first { it.id == 1L }.isExpanded)

        // Toggle card 2 off
        vm.toggleCardExpansion(2L)
        assertFalse(vm.uiState.value.expandedCardIds.contains(2L))
        assertFalse(vm.uiState.value.notifications.first { it.id == 2L }.isExpanded)
        assertFalse(vm.uiState.value.filteredNotifications.first { it.id == 2L }.isExpanded)
    }

    @Test
    fun computeListenerState_evaluatesAllCombinationsCorrectly() {
        // Active: Service enabled and connected
        assertEquals(
            ListenerState.Active,
            viewModel.computeListenerState(isServiceEnabled = true, isConnected = true, hasNotifications = false)
        )
        assertEquals(
            ListenerState.Active,
            viewModel.computeListenerState(isServiceEnabled = true, isConnected = true, hasNotifications = true)
        )

        // Paused: Service disabled but has notifications
        assertEquals(
            ListenerState.Paused,
            viewModel.computeListenerState(isServiceEnabled = false, isConnected = false, hasNotifications = true)
        )

        // Inactive: Service disabled and no notifications
        assertEquals(
            ListenerState.Inactive,
            viewModel.computeListenerState(isServiceEnabled = false, isConnected = false, hasNotifications = false)
        )

        // Paused: Service enabled but disconnected and has notifications
        assertEquals(
            ListenerState.Paused,
            viewModel.computeListenerState(isServiceEnabled = true, isConnected = false, hasNotifications = true)
        )

        // Inactive: Service enabled but disconnected and no notifications
        assertEquals(
            ListenerState.Inactive,
            viewModel.computeListenerState(isServiceEnabled = true, isConnected = false, hasNotifications = false)
        )
    }
}
