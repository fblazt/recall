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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notificationhistory.ui.theme.AmberWarningAccent
import com.notificationhistory.ui.theme.AmberWarningBackground
import com.notificationhistory.ui.theme.AmberWarningBorder
import com.notificationhistory.ui.theme.AmberWarningText
import com.notificationhistory.ui.theme.NotificationHistoryTheme

@Composable
fun AccessRevokedBanner(
    onFixClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AmberWarningBackground)
            .border(
                width = 1.dp,
                color = AmberWarningBorder,
                shape = RoundedCornerShape(24.dp)
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
                    .background(AmberWarningAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarningAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Listener service paused",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = AmberWarningText,
                    lineHeight = 20.sp
                )
                Text(
                    text = "New notifications are not being recorded because permission was revoked.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp
                    ),
                    color = AmberWarningText.copy(alpha = 0.7f)
                )
            }
        }

        Surface(
            onClick = onFixClick,
            shape = CircleShape,
            color = AmberWarningAccent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, AmberWarningBorder),
            modifier = Modifier.align(Alignment.Start)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Fix in settings",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AmberWarningText
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AmberWarningText,
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

