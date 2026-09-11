package com.notificationhistory.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isSetupCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun observeSetupCompleted(): Flow<Boolean> = callbackFlow {
        trySend(isSetupCompleted())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SETUP_COMPLETED) {
                trySend(isSetupCompleted())
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    companion object {
        const val PREFS_NAME = "notification_history_preferences"
        const val KEY_SETUP_COMPLETED = "setup_completed"
    }
}
