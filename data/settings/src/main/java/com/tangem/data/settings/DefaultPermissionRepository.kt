package com.tangem.data.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowInitialPermissionScreen
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowPermission
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.PermissionRepository

internal class DefaultPermissionRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val context: Context,
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

    override fun hasRuntimePermission(permission: String): Boolean {
        return context.isPermissionGranted(permission)
    }

    private fun Context.isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}