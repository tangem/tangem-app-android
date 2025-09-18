package com.tangem.tap.di

import com.tangem.tap.common.buildconfig.AppConfigurationProviderImpl
import com.tangem.utils.buildConfig.AppConfigurationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppConfigurationModule {

    @Binds
    @Singleton
    fun bindAppConfigurationProvider(impl: AppConfigurationProviderImpl): AppConfigurationProvider
}