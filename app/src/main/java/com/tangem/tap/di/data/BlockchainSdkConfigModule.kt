package com.tangem.tap.di.data

import com.tangem.blockchain.common.BlockchainSdkConfig
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BlockchainSdkConfigModule {

    @Provides
    @Singleton
    fun provideBlockchainSdkConfig(environmentConfig: EnvironmentConfig): BlockchainSdkConfig {
        return environmentConfig.blockchainSdkConfig
    }
}