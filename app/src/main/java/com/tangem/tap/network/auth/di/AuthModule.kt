package com.tangem.tap.network.auth.di

import com.tangem.lib.auth.AuthProvider
import com.tangem.tap.network.auth.AuthProviderImpl
import com.tangem.tap.proxy.AppStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AuthModule {

    @Provides
    @Singleton
    fun provideAuthProvider(appStateHolder: AppStateHolder): AuthProvider {
        return AuthProviderImpl(appStateHolder)
    }
}
