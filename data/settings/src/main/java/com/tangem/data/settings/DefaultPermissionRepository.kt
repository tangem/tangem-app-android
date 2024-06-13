package com.tangem.data.settings

import android.os.SystemClock
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.PreferencesKeys.getIsFirstTimeAskingPermission
import com.tangem.datasource.local.preferences.PreferencesKeys.getPermissionDaysCount
import com.tangem.datasource.local.preferences.PreferencesKeys.getPermissionLaunchCount
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowInitialPermissionScreen
import com.tangem.datasource.local.preferences.PreferencesKeys.getShouldShowPermission
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.PermissionRepository

internal class DefaultPermissionRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PermissionRepository {

    override suspend fun shouldInitiallyShowPermissionScreen(permission: Int): Boolean {
        val key = getShouldShowInitialPermissionScreen(permission)
        val initialPermissionScreen = appPreferencesStore.getSyncOrDefault(key = key, default = true)
        if (initialPermissionScreen) appPreferencesStore.store(key = key, value = false)
        return initialPermissionScreen
    }

    override suspend fun isFirstTimeAskingPermission(permission: Int): Boolean = appPreferencesStore.getSyncOrDefault(
        key = getIsFirstTimeAskingPermission(permission),
        default = true,
    )

    override suspend fun setFirstTimeAskingPermission(permission: Int, value: Boolean) {
        appPreferencesStore.store(
            key = getIsFirstTimeAskingPermission(permission),
            value = value,
        )
    }

    override suspend fun shouldAskPermission(permission: Int): Boolean {
        val shouldAskPermission = appPreferencesStore.getSyncOrDefault(getShouldShowPermission(permission), true)
        val delayedLaunches = appPreferencesStore.getSyncOrDefault(getPermissionLaunchCount(permission), 0)
        val delayedDays = appPreferencesStore.getSyncOrDefault(getPermissionDaysCount(permission), 0)
        val currentLaunchCounter = appPreferencesStore.getSyncOrDefault(PreferencesKeys.APP_LAUNCH_COUNT_KEY, 0)

        val nowMillis = SystemClock.elapsedRealtime()
        val isDaysDelayed = delayedDays < nowMillis
        val isLaunchesDelayed = delayedLaunches < currentLaunchCounter
        return shouldAskPermission && isDaysDelayed && isLaunchesDelayed
    }

    override suspend fun neverAskPermission(permission: Int) {
        appPreferencesStore.store(key = getShouldShowPermission(permission), value = false)
    }

    override suspend fun delayPermissionAsking(permission: Int) {
        appPreferencesStore.editData {
            val appLaunchCounter = it.getOrDefault(PreferencesKeys.APP_LAUNCH_COUNT_KEY, 0)
            val nowMillis = SystemClock.elapsedRealtime()

            it[getPermissionLaunchCount(permission)] = appLaunchCounter + DELAY_LAUNCH_COUNT
            it[getPermissionDaysCount(permission)] = nowMillis + DELAY_DAYS_COUNT
        }
    }

    private companion object {
        const val DELAY_LAUNCH_COUNT = 5
        const val DELAY_DAYS_COUNT = 3L * 24 * 3600 * 1000 // 3 days in millis
    }
}