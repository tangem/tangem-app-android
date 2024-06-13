package com.tangem.domain.settings.repositories

interface PermissionRepository {

    /**
     * Return true if should display screen ONCE asking to allow [permission].
     * False otherwise or screen already was displayed
     */
    suspend fun shouldInitiallyShowPermissionScreen(permission: Int): Boolean

    /**
     * Indicates which time [permission] was asked via platform dialog.
     * NOTE: Use this method to indicate either reroute to settings or display platform dialog.
     */
    suspend fun isFirstTimeAskingPermission(permission: Int): Boolean

    /**
     * Sets value indicating that [permission] was asked via platform dialog.
     * NOTE: Use this method to indicate either reroute to settings or display platform dialog.
     */
    suspend fun setFirstTimeAskingPermission(permission: Int, value: Boolean)

    /**
     * Is clear to ask [permission].
     * User could already granted or permanently denied permission
     * Or there is an active delay before next request
     */
    suspend fun shouldAskPermission(permission: Int): Boolean

    /**
     * Permanently deny [permission] and never request again
     */
    suspend fun neverAskPermission(permission: Int)

    /**
     * Delay next [permission] request for some time or active sessions
     */
    suspend fun delayPermissionAsking(permission: Int)
}