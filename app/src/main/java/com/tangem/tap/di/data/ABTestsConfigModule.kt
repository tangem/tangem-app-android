package com.tangem.tap.di.data

import com.tangem.core.abtests.ABTestsConfig
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ABTestsConfigModule {

    @Provides
    @Singleton
    fun provideABTestsConfig(storeEnvironmentConfig: StoreEnvironmentConfig): ABTestsConfig {
        return ABTestsConfig(
            amplitudeApiKey = storeEnvironmentConfig.amplitudeApiKey,
            amplitudeApiKeyDev = storeEnvironmentConfig.amplitudeApiKeyDev,
        )
    }
}