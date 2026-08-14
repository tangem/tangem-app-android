package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.spend.datasource.config.TangemPayEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemPayConfigModule {

    @Provides
    @Singleton
    fun provideTangemPayEnvironmentConfig(environmentConfig: EnvironmentConfig): TangemPayEnvironmentConfig {
        return TangemPayEnvironmentConfig(
            bffStaticToken = environmentConfig.bffStaticToken,
            bffStaticTokenDev = environmentConfig.bffStaticTokenDev,
        )
    }
}