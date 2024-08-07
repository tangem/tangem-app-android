package com.tangem.data.settings

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowInitialPermissionScreen
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowPermission
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.PermissionRepository

internal class DefaultPermissionRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PermissionRepository {

    override suspend fun shouldInitiallyShowPermissionScreen(permission: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = getShouldShowInitialPermissionScreen(permission),
            default = true,
        )
    }

    override suspend fun neverInitiallyShowPermissionScreen(permission: String) {
        appPreferencesStore.store(
            key = getShouldShowInitialPermissionScreen(permission),
            value = false,
        )
    }

    override suspend fun shouldAskPermission(permission: String): Boolean {
        return appPreferencesStore.getSyncOrDefault(getShouldShowPermission(permission), true)
    }

    override suspend fun neverAskPermission(permission: String) {
        appPreferencesStore.store(key = getShouldShowPermission(permission), value = false)
    }
}
