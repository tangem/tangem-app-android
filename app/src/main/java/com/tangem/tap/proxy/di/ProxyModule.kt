package com.tangem.tap.proxy.di

import com.tangem.tap.proxy.AppStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ProxyModule {

    @Provides
    @Singleton
    fun provideAppStateHolder(): AppStateHolder {
        return AppStateHolder()
    }
}