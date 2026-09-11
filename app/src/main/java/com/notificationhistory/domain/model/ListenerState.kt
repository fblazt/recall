package com.notificationhistory.domain.model

sealed interface ListenerState {
    data object Active : ListenerState
    data object Paused : ListenerState
    data object Inactive : ListenerState
}
