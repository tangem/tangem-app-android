package com.tangem.tap.network.auth.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.P2PEthPoolAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.tap.network.auth.*
import com.tangem.utils.version.AppVersionProvider
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
        environmentConfigStorage: EnvironmentConfigStorage,
    ): AuthProvider {
        return DefaultAuthProvider(
            userWalletsListRepository = userWalletsListRepository,
            environmentConfigStorage = environmentConfigStorage,
        )
    }

    @Provides
    @Singleton
    fun provideExpressAuthProvider(): ExpressAuthProvider {
        return DefaultExpressAuthProvider()
    }

    @Provides
    @Singleton
    fun provideStakeKitAuthProvider(environmentConfigStorage: EnvironmentConfigStorage): StakeKitAuthProvider {
        return DefaultStakeKitAuthProvider(environmentConfigStorage)
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolAuthProvider(environmentConfigStorage: EnvironmentConfigStorage): P2PEthPoolAuthProvider {
        return DefaultP2PEthPoolAuthProvider(environmentConfigStorage)
    }

    @Provides
    @Singleton
    fun provideAppVersionProvider(): AppVersionProvider {
        return DefaultAppVersionProvider()
    }
}