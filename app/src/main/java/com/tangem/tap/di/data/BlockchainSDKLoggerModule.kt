package com.tangem.tap.di.data

import com.tangem.blockchain.common.logging.BlockchainSDKLogger
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.tap.data.TangemBlockchainSDKLogger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockchainSDKLoggerModule {

    @Provides
    @Singleton
    fun provideBlockchainSDKLogger(
        settingsRepository: SettingsRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): BlockchainSDKLogger {
        return TangemBlockchainSDKLogger(settingsRepository, dispatchers)
    }
}