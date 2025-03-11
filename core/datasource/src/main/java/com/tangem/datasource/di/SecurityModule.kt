package com.tangem.datasource.di

import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.crypto.Sha256SignatureVerifier
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
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
        environmentConfigStorage: EnvironmentConfigStorage,
        apiConfigsManager: ApiConfigsManager,
    ): DataSignatureVerifier {
        return Sha256SignatureVerifier(environmentConfigStorage, apiConfigsManager)
    }
}