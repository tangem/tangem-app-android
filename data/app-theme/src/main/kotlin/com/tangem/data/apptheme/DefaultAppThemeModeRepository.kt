package com.tangem.data.apptheme

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import kotlinx.coroutines.flow.Flow

internal class DefaultAppThemeModeRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : AppThemeModeRepository {

    override fun getAppThemeMode(): Flow<AppThemeMode> {
        return appPreferencesStore.getObject(
            key = storePreferencesKeys.appThemeModeKey,
            default = AppThemeMode.DEFAULT,
        )
    }

    override suspend fun changeAppThemeMode(mode: AppThemeMode) {
        appPreferencesStore.storeObject(key = storePreferencesKeys.appThemeModeKey, value = mode)
    }
}