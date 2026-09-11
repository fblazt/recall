package com.notificationhistory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notificationhistory.ui.theme.NotificationHistoryTheme

/**
 * Amber warning banner displayed when notification listener access permission has been revoked or restricted.
 *
 * Matching Stitch UI specs:
 * - Background: #2A2012 (amber dark container)
 * - Border: 1.dp #4DFBBF24
 * - Shape: RoundedCornerShape(16.dp)
 * - Leading warning icon: 32.dp container (rounded 10.dp, bg #26FBBF24, icon #FBBF24)
 * - Title: "Listener service paused. New notifications are not being recorded." (#FEF3C7)
 * - Subtitle: "Notification access permission was revoked or restricted." (#FDE68A 70%)
 * - Pill button: "Fix in Settings" with trailing arrow forward, triggers [onFixClick]
 */
@Composable
fun AccessRevokedBanner(
    onFixClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerBackground = Color(0xFF2A2012)
    val amberBorder = Color(0x4DFBBF24)
    val amberContainer = Color(0x26FBBF24)
    val amberAccent = Color(0xFFFBBF24)
    val titleColor = Color(0xFFFEF3C7)
    val subtitleColor = Color(0xFFFDE68A).copy(alpha = 0.7f)
    val buttonTextColor = Color(0xFFFCD34D)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bannerBackground)
            .border(
                width = 1.dp,
                color = amberBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(amberContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = amberAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Listener service paused. New notifications are not being recorded.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = titleColor,
                    lineHeight = 20.sp
                )
                Text(
                    text = "Notification access permission was revoked or restricted.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = subtitleColor,
                    lineHeight = 16.sp
                )
            }
        }

        Surface(
            onClick = onFixClick,
            shape = CircleShape,
            color = amberContainer,
            border = BorderStroke(1.dp, amberBorder),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Fix in Settings",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = buttonTextColor
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = buttonTextColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(name = "Access Revoked Banner", showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun AccessRevokedBannerPreview() {
    NotificationHistoryTheme {
        AccessRevokedBanner(
            onFixClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
