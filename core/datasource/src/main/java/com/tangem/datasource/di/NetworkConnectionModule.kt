package com.tangem.datasource.di

import android.content.Context
import com.tangem.datasource.connection.AndroidNetworkConnectionManager
import com.tangem.datasource.connection.NetworkConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class NetworkConnectionModule {

    @Provides
    @Singleton
    fun provideNetworkConnectionManager(@ApplicationContext applicationContext: Context): NetworkConnectionManager {
        return AndroidNetworkConnectionManager(applicationContext = applicationContext)
    }
}