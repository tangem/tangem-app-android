package com.tangem.datasource.di

import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.config.ConfigManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ConfigModule {

    @Binds
    fun bindConfigManager(configManager: ConfigManagerImpl): ConfigManager
}
