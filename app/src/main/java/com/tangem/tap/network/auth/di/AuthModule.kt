package com.tangem.tap.network.auth.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.datasource.api.auth.ExpressAuthProvider
import com.tangem.datasource.api.auth.P2PEthPoolAuthProvider
import com.tangem.datasource.api.auth.StakeKitAuthProvider
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
        environmentConfig: EnvironmentConfig,
    ): AuthProvider {
        return DefaultAuthProvider(
            userWalletsListRepository = userWalletsListRepository,
            environmentConfig = environmentConfig,
        )
    }

    @Provides
    @Singleton
    fun provideExpressAuthProvider(): ExpressAuthProvider {
        return DefaultExpressAuthProvider()
    }

    @Provides
    @Singleton
    fun provideStakeKitAuthProvider(environmentConfig: EnvironmentConfig): StakeKitAuthProvider {
        return DefaultStakeKitAuthProvider(environmentConfig)
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolAuthProvider(environmentConfig: EnvironmentConfig): P2PEthPoolAuthProvider {
        return DefaultP2PEthPoolAuthProvider(environmentConfig)
    }
}