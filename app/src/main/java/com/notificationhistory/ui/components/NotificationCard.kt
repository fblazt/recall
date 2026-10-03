package com.notificationhistory.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.notificationhistory.domain.model.NotificationRecord
import com.notificationhistory.ui.theme.NotificationHistoryTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a notification post timestamp in milliseconds into a user-friendly string.
 * - Today: "10:42 AM"
 * - Yesterday: "Yesterday, 10:42 AM"
 * - Current year: "Oct 12, 10:42 AM"
 * - Other years: "Oct 12, 2025, 10:42 AM"
 */
fun formatNotificationTime(
    postTimeMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (postTimeMillis <= 0L) return ""
    return runCatching {
        val instant = Instant.ofEpochMilli(postTimeMillis)
        val zonedDateTime = instant.atZone(zoneId)
        val today = LocalDate.now(zoneId)
        val recordDate = zonedDateTime.toLocalDate()

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val timeString = zonedDateTime.format(timeFormatter)

        when {
            recordDate == today -> timeString
            recordDate == today.minusDays(1) -> "Yesterday, $timeString"
            recordDate.year == today.year -> {
                val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                "${zonedDateTime.format(dateFormatter)}, $timeString"
            }
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                "${zonedDateTime.format(dateFormatter)}, $timeString"
            }
        }
    }.getOrDefault("")
}

/**
 * Creates an [AnnotatedString] with highlighted substring matches for [query].
 * Matches are case-insensitive. If [query] is blank or no matches are found,
 * the original [text] is returned unmodified.
 */
@Composable
fun highlightSearchQuery(
    text: String,
    query: String,
    highlightBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    highlightTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
): AnnotatedString {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty() || text.isEmpty()) {
        return AnnotatedString(text)
    }

    return remember(text, trimmedQuery, highlightBackgroundColor, highlightTextColor) {
        val matches = mutableListOf<Pair<Int, Int>>()
        var startIndex = 0
        while (startIndex < text.length) {
            val foundIndex = text.indexOf(trimmedQuery, startIndex, ignoreCase = true)
            if (foundIndex == -1) break
            val endIndex = foundIndex + trimmedQuery.length
            matches.add(foundIndex to endIndex)
            startIndex = endIndex
        }

        if (matches.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                for ((start, end) in matches) {
                    addStyle(
                        style = SpanStyle(
                            background = highlightBackgroundColor,
                            color = highlightTextColor,
                            fontWeight = FontWeight.SemiBold
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Notification Card displaying notification header, payload content,
 * and optional metadata expansion matching the stitch design specification.
 */
@Composable
fun NotificationCard(
    record: NotificationRecord,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    metadataContent: @Composable (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onToggleExpand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize()
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon container (32.dp circular avatar or fallback)
                val context = LocalContext.current
                val appIcon = remember(record.packageName) {
                    runCatching {
                        val drawable = context.packageManager.getApplicationIcon(record.packageName)
                        drawable.toBitmap(width = 64, height = 64).asImageBitmap()
                    }.getOrNull()
                }
                val displayAppName = if (record.appName.isNotBlank()) record.appName else record.packageName
                val initial = remember(displayAppName) {
                    displayAppName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                }

                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // App title: Text(record.appName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    text = displayAppName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val isMatching = remember(record, searchQuery) {
                    searchQuery.isNotBlank() && (
                        record.title.contains(searchQuery.trim(), ignoreCase = true) ||
                        record.text.contains(searchQuery.trim(), ignoreCase = true) ||
                        record.appName.contains(searchQuery.trim(), ignoreCase = true)
                    )
                }
                if (isMatching) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MATCH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Time string: formatted time, labelSmall, color onSurfaceVariant
                val formattedTime = remember(record.postTime) {
                    formatNotificationTime(record.postTime)
                }
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Chevron icon: KeyboardArrowDown with animateFloatAsState rotation
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "chevron_rotation"
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse notification details" else "Expand notification details",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content column
            if (record.title.isNotBlank() || record.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (record.title.isNotBlank()) {
                        Text(
                            text = highlightSearchQuery(
                                text = record.title,
                                query = searchQuery
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (record.text.isNotBlank()) {
                        Text(
                            text = highlightSearchQuery(
                                text = record.text,
                                query = searchQuery
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expanded metadata content slot
            if (isExpanded && metadataContent != null) {
                Spacer(modifier = Modifier.height(12.dp))
                metadataContent()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun NotificationCardCollapsedPreview() {
    NotificationHistoryTheme {
        NotificationCard(
            record = NotificationRecord(
                id = 1L,
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Security alert",
                text = "New device signed in to your Google Account. Check activity now to ensure it was you.",
                postTime = System.currentTimeMillis(),
                notificationKey = "key_1",
                category = "CATEGORY_EMAIL"
            ),
            isExpanded = false,
            onToggleExpand = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun NotificationCardExpandedPreview() {
    NotificationHistoryTheme {
        NotificationCard(
            record = NotificationRecord(
                id = 2L,
                packageName = "com.slack",
                appName = "Slack",
                title = "#dev-team: Deployment finished",
                text = "Version 1.2.0 has been deployed to production. All automated health checks returned status 200 OK without errors.",
                postTime = System.currentTimeMillis() - 3600000L,
                notificationKey = "key_2",
                category = "CATEGORY_MESSAGE"
            ),
            isExpanded = true,
            onToggleExpand = {},
            metadataContent = {
                Text(
                    text = "Key: key_2 | Package: com.slack",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun NotificationCardHighlightedPreview() {
    NotificationHistoryTheme {
        NotificationCard(
            record = NotificationRecord(
                id = 3L,
                packageName = "com.google.android.gm",
                appName = "Gmail",
                title = "Security alert for your account",
                text = "New device signed in to your Google Account. Check alert now to ensure it was you.",
                postTime = System.currentTimeMillis(),
                notificationKey = "key_3",
                category = "CATEGORY_EMAIL"
            ),
            isExpanded = false,
            searchQuery = "alert",
            onToggleExpand = {}
        )
    }
}

