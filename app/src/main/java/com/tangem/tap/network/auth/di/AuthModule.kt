package com.tangem.tap.network.auth.di

import com.tangem.datasource.remote.auth.AuthProvider
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import com.tangem.tap.network.auth.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class AuthModule {

    @Provides
    @Singleton
    fun provideAuthProvider(
        userWalletsListRepository: UserWalletsListRepository,
        storeEnvironmentConfig: StoreEnvironmentConfig,
    ): AuthProvider {
        return DefaultAuthProvider(
            userWalletsListRepository = userWalletsListRepository,
            storeEnvironmentConfig = storeEnvironmentConfig,
        )
    }
}