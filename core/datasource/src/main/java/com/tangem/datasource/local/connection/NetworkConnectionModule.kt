package com.tangem.datasource.local.connection

import com.tangem.core.local.connection.NetworkConnectionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkConnectionModule {

    @Binds
    fun bindNetworkConnectionManager(impl: AndroidNetworkConnectionManager): NetworkConnectionManager
}