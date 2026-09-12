package com.notificationhistory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notificationhistory.domain.model.ListenerState
import com.notificationhistory.ui.theme.NotificationHistoryTheme

/**
 * Format the notification count summary string for the header.
 */
fun formatNotificationSummary(capturedCount: Int): String {
    return if (capturedCount == 1) {
        "1 notification • Last 3 days"
    } else {
        "$capturedCount notifications • Last 3 days"
    }
}

/**
 * Header status indicator displaying the captured notification summary row.
 *
 * Matching Stitch UI specs:
 * Borderless row with an outlined inbox icon followed by "$capturedCount notifications • Last 3 days".
 */
@Composable
fun ServiceStatusHeader(
    capturedCount: Int,
    modifier: Modifier = Modifier,
    listenerState: ListenerState = ListenerState.Active
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = formatNotificationSummary(capturedCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(name = "Multiple Notifications", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderMultiplePreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            capturedCount = 42,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Single Notification", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderSinglePreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            capturedCount = 1,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Zero Notifications", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ServiceStatusHeaderZeroPreview() {
    NotificationHistoryTheme {
        ServiceStatusHeader(
            capturedCount = 0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
