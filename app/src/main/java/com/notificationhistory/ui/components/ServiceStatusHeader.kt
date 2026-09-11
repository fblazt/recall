package com.notificationhistory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.ui.theme.NotificationHistoryTheme

/**
 * Header status indicator displaying the current [ListenerState] and captured notification count.
 *
 * Matching Stitch UI specs:
 * - Active: 10.dp emerald dot (#34D399), "Listener service active" in onSurface
 * - Paused: 10.dp amber dot (#FBBF24), "Listener service paused" in #FDE68A
 * - Inactive: 10.dp dot (outline 60%), "Listener service inactive" in onSurfaceVariant
 * - Right side: "Rolling 72h • $capturedCount captured" (or "$capturedCount captured" if inactive)
 */
@Composable
fun ServiceStatusHeader(
    listenerState: ListenerState,
    capturedCount: Int,
    modifier: Modifier = Modifier
) {
    val (dotColor, statusText, statusTextColor) = when (listenerState) {
        is ListenerState.Active -> Triple(
            Color(0xFF34D399),
            "Listener service active",
            MaterialTheme.colorScheme.onSurface
        )
        is ListenerState.Paused -> Triple(
            Color(0xFFFBBF24),
            "Listener service paused",
            Color(0xFFFDE68A)
        )
        is ListenerState.Inactive -> Triple(
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            "Listener service inactive",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val countText = if (listenerState is ListenerState.Inactive) {
        "$capturedCount captured"
    } else {
        "Rolling 72h • $capturedCount captured"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = statusText,
                color = statusTextColor,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Text(
            text = countText,
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(name = "Active State", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderActivePreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            listenerState = ListenerState.Active,
            capturedCount = 42,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Paused State", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderPausedPreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            listenerState = ListenerState.Paused,
            capturedCount = 42,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Inactive State", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderInactivePreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            listenerState = ListenerState.Inactive,
            capturedCount = 0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
