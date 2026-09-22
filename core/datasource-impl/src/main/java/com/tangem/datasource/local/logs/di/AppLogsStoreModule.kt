package com.tangem.datasource.local.logs.di

import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.logs.DefaultAppLogsStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppLogsStoreModule {

    @Binds
    @Singleton
    fun bindAppLogsStore(impl: DefaultAppLogsStore): AppLogsStore
}