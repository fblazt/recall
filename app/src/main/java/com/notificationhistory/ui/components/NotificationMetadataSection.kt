package com.notificationhistory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notificationhistory.domain.model.NotificationRecord
import com.notificationhistory.ui.theme.NotificationHistoryTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a timestamp into an exact date and time string matching the metadata drawer spec:
 * e.g., "Oct 24, 2023, 10:42:15 AM".
 */
fun formatMetadataTimestamp(
    postTimeMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String {
    if (postTimeMillis <= 0L) return ""
    return runCatching {
        val instant = Instant.ofEpochMilli(postTimeMillis)
        val zonedDateTime = instant.atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a", locale)
        zonedDateTime.format(formatter)
    }.getOrDefault("")
}

/**
 * Material 3 Notification Metadata Drawer component displaying technical notification
 * metadata (exact post time, package name, and Android notification key) in selectable
 * monospaced typography matching the stitch design specification.
 */
@Composable
fun NotificationMetadataSection(
    record: NotificationRecord,
    modifier: Modifier = Modifier
) {
    val monoTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
    val formattedTime = remember(record.postTime) {
        formatMetadataTimestamp(record.postTime).ifBlank { record.postTime.toString() }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        SelectionContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Exact timestamp
                MetadataRow(
                    label = "Time",
                    value = formattedTime,
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    monoTextStyle = monoTextStyle
                )

                // Row 2: Package name
                MetadataRow(
                    label = "Package",
                    value = record.packageName,
                    valueColor = MaterialTheme.colorScheme.primary,
                    monoTextStyle = monoTextStyle
                )

                // Row 3: Android Notification Key
                MetadataRow(
                    label = "Key",
                    value = record.notificationKey,
                    valueColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    monoTextStyle = monoTextStyle
                )
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    valueColor: Color,
    monoTextStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = monoTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = monoTextStyle,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "NotificationMetadataSection - Dark Theme", showBackground = true, backgroundColor = 0xFF1E2025)
@Composable
private fun NotificationMetadataSectionPreview() {
    NotificationHistoryTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NotificationMetadataSection(
                record = NotificationRecord(
                    id = 1L,
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    title = "Sarah Jenkins",
                    text = "Hey team, the revised UI mockups are ready for review.",
                    postTime = 1698144135000L,
                    notificationKey = "0|com.whatsapp|1002|null|10142",
                    category = "CATEGORY_MESSAGE"
                )
            )
        }
    }
}
