package com.tangem.datasource.di.local.config

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.converter.GeneratedEnvironmentConfigConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ConfigModule {

    @Provides
    @Singleton
    fun provideEnvironmentConfig(): EnvironmentConfig {
        return GeneratedEnvironmentConfigConverter.convert()
    }

    @Provides
    @Singleton
    @Named("authServiceKey")
    fun provideAuthServiceKey(environmentConfig: EnvironmentConfig): String? = environmentConfig.authServiceKey
}