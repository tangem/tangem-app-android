package com.tangem.domain.settings.repositories

interface PermissionRepository {

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
     * Return is [permission] granted or not.
     */
    fun hasRuntimePermission(permission: String): Boolean
}