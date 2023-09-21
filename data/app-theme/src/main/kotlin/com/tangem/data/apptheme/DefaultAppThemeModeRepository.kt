package com.tangem.data.apptheme

import com.tangem.datasource.local.apptheme.AppThemeModeStore
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.apptheme.repository.AppThemeModeRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DefaultAppThemeModeRepository(
    private val appThemeModeStore: AppThemeModeStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppThemeModeRepository {

    override fun getAppThemeMode(): Flow<AppThemeMode> {
        return channelFlow {
            launch(dispatchers.io) {
                if (appThemeModeStore.isEmpty()) {
                    appThemeModeStore.store(AppThemeMode.DEFAULT)
                }
            }

            launch(dispatchers.io) {
                appThemeModeStore.get().collect(::send)
            }
        }
    }

    override suspend fun changeAppThemeMode(mode: AppThemeMode) {
        withContext(dispatchers.io) {
            appThemeModeStore.store(mode)
        }
    }
}