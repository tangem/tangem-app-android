package com.tangem.datasource.di

import com.tangem.datasource.connection.AndroidNetworkConnectionManager
import com.tangem.datasource.connection.NetworkConnectionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkConnectionModule {

    @Binds
    @Singleton
    fun bindNetworkConnectionManager(
        androidNetworkConnectionManager: AndroidNetworkConnectionManager,
    ): NetworkConnectionManager
}