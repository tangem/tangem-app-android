package com.tangem.grow.datasource.di

import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.config.GrowEnvironmentConfig
import com.tangem.grow.datasource.crypto.DataSignatureVerifier
import com.tangem.grow.datasource.crypto.MockDataSignatureVerifier
import com.tangem.grow.datasource.crypto.Sha256SignatureVerifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SecurityModule {

    @Provides
    @Singleton
    fun provideDataSignatureVerifier(
        growEnvironmentConfig: GrowEnvironmentConfig,
        apiConfigsManager: ApiConfigsManager,
    ): DataSignatureVerifier {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockDataSignatureVerifier()
        } else {
            Sha256SignatureVerifier(growEnvironmentConfig, apiConfigsManager)
        }
    }
}