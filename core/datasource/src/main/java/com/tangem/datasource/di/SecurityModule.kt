package com.tangem.datasource.di

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.crypto.Sha256SignatureVerifier
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
    fun provideDataSignatureVerifier(configManager: ConfigManager): DataSignatureVerifier {
        return Sha256SignatureVerifier(configManager)
    }
}