package com.tangem.datasource.di

import com.tangem.datasource.connection.NetworkConnectionManager
import com.tangem.datasource.connection.RealInternetConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class NetworkConnectionModule {

    @Provides
    @Singleton
    fun provideNetworkConnectionManager(): NetworkConnectionManager {
        return RealInternetConnectionManager()
    }
}