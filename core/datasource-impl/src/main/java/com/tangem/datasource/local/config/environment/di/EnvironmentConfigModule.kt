package com.tangem.datasource.local.config.environment.di

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.converter.GeneratedEnvironmentConfigConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EnvironmentConfigModule {

    @Provides
    @Singleton
    fun provideEnvironmentConfig(): EnvironmentConfig {
        return GeneratedEnvironmentConfigConverter.convert()
    }
}