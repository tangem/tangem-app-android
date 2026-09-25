package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.walletconnect.config.WalletConnectConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletConnectConfigModule {

    @Provides
    @Singleton
    fun provideWalletConnectConfig(environmentConfig: EnvironmentConfig): WalletConnectConfig {
        return WalletConnectConfig(projectId = environmentConfig.walletConnectProjectId)
    }
}