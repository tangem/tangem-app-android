package com.tangem.datasource.di

import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.blockchain.DefaultBlockchainDataStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockchainWalletManagerFactoryModule {

    @Provides
    @Singleton
    fun provideBlockchainWalletManagerFactory(
        configManager: ConfigManager,
        appPreferencesStore: AppPreferencesStore,
    ): WalletManagerFactory {
        return WalletManagerFactory(
            config = configManager.config.blockchainSdkConfig,
            blockchainDataStorage = DefaultBlockchainDataStorage(appPreferencesStore),
        )
    }
}