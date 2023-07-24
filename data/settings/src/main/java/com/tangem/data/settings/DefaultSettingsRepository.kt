package com.tangem.data.settings

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultSettingsRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: CoroutineDispatcherProvider,
) : SettingsRepository {

    override suspend fun isUserAlreadyRateApp(): Boolean {
        return withContext(dispatchers.io) {
            preferencesDataSource.appRatingLaunchObserver.isReadyToShow()
        }
    }
}
