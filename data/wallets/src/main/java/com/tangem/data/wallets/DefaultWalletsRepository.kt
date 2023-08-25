package com.tangem.data.wallets

import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultWalletsRepository(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletsRepository {

    override suspend fun shouldSaveUserWallets(): Boolean {
        return withContext(dispatchers.io) { preferencesDataSource.shouldSaveUserWallets }
    }
}