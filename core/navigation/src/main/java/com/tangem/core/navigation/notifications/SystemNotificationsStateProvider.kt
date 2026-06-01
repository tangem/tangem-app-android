package com.tangem.core.navigation.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around [NotificationManagerCompat.areNotificationsEnabled] for OS-level notification toggle state.
 *
 * Reflects the user's preference in system settings (independent of runtime POST_NOTIFICATIONS permission
 * on Android 13+). Returns `false` if notifications are blocked at the OS level.
 */
@Singleton
class SystemNotificationsStateProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}