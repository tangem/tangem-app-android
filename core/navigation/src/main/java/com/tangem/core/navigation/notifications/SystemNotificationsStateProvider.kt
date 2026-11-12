package com.tangem.core.navigation.notifications

/**
 * OS-level notification toggle state.
 *
 * Reflects the user's preference in system settings (independent of runtime POST_NOTIFICATIONS permission
 * on Android 13+).
 */
interface SystemNotificationsStateProvider {

    /** Returns `false` if notifications are blocked at the OS level. */
    fun areNotificationsEnabled(): Boolean
}