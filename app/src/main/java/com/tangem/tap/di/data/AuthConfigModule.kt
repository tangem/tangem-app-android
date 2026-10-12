package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.generated.GeneratedEnvironmentConfig
import com.tangem.lib.auth.api.config.AuthServiceConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AuthConfigModule {

    @Provides
    @Singleton
    fun provideAuthServiceConfig(): AuthServiceConfig {
        return AuthServiceConfig(authServiceKey = GeneratedEnvironmentConfig.authServiceKey)
    }
}