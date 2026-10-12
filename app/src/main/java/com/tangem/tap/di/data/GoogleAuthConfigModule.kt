package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig
import com.tangem.google.auth.GoogleAuthConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GoogleAuthConfigModule {

    @Provides
    @Singleton
    fun provideGoogleAuthConfig(): GoogleAuthConfig {
        return GoogleAuthConfig(webClientId = GeneratedEnvironmentConfig.googleWebClientId)
    }
}