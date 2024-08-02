package com.tangem.domain.settings.repositories

interface PermissionRepository {

    /**
     * Return true if should display screen ONCE asking to allow [permission].
     * False otherwise or screen already was displayed
     */
    suspend fun shouldInitiallyShowPermissionScreen(permission: String): Boolean

    /**
     * Sets value indicating that screen for [permission] was shown in initial app launch
     */
    suspend fun neverInitiallyShowPermissionScreen(permission: String)

    /**
     * Indicates which time [permission] was asked via platform dialog.
     * NOTE: Use this method to indicate either reroute to settings or display platform dialog.
     */
    suspend fun isFirstTimeAskingPermission(permission: String): Boolean

    /**
     * Sets value indicating that [permission] was asked via platform dialog.
     * NOTE: Use this method to indicate either reroute to settings or display platform dialog.
     */
    suspend fun setFirstTimeAskingPermission(permission: String, value: Boolean)

    /**
     * Is clear to ask [permission].
     * User could already granted or permanently denied permission
     * Or there is an active delay before next request
     */
    suspend fun shouldAskPermission(permission: String): Boolean

    /**
     * Permanently deny [permission] and never request again
     */
    suspend fun neverAskPermission(permission: String)

    /**
     * Delay next [permission] request for some time or active sessions
     */
    suspend fun delayPermissionAsking(permission: String)
}
