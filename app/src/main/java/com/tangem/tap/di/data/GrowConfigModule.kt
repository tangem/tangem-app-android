package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GrowConfigModule {

    @Provides
    @Singleton
    fun provideGrowEnvironmentConfig(environmentConfig: EnvironmentConfig): GrowEnvironmentConfig {
        return GrowEnvironmentConfig(
            moonPayApiKey = environmentConfig.moonPayApiKey,
            moonPayApiSecretKey = environmentConfig.moonPayApiSecretKey,
            mercuryoWidgetId = environmentConfig.mercuryoWidgetId,
            mercuryoSecret = environmentConfig.mercuryoSecret,
            stakeKitApiKey = environmentConfig.stakeKitApiKey,
            yieldModuleApiKey = environmentConfig.yieldModuleApiKey,
            yieldModuleApiKeyDev = environmentConfig.yieldModuleApiKeyDev,
            gaslessTxApiKey = environmentConfig.gaslessTxApiKey,
            gaslessTxApiKeyDev = environmentConfig.gaslessTxApiKeyDev,
        )
    }
}