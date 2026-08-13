package com.tangem.datasource.di

import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.DefaultAppDataStoreFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppDataStoreFactoryModule {

    @Binds
    @Singleton
    fun bindAppDataStoreFactory(impl: DefaultAppDataStoreFactory): AppDataStoreFactory
}