package com.notificationhistory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.notificationhistory.ui.NotificationHistoryScreen
import com.notificationhistory.ui.theme.NotificationHistoryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NotificationHistoryTheme {
                NotificationHistoryScreen()
            }
        }
    }
}
