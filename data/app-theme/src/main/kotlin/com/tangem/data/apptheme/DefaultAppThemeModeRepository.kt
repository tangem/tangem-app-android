package com.tangem.data.apptheme

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultAppThemeModeRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : AppThemeModeRepository {

    override fun getAppThemeMode(): Flow<AppThemeMode> {
        return appPreferencesStore.getObject(
            key = PreferencesKeys.APP_THEME_MODE_KEY,
            default = AppThemeMode.DEFAULT,
        )
    }

    override suspend fun changeAppThemeMode(mode: AppThemeMode) {
        appPreferencesStore.storeObject(key = PreferencesKeys.APP_THEME_MODE_KEY, value = mode)
    }
}