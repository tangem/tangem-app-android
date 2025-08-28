package com.tangem.tap.network.auth.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.tap.network.auth.DefaultAppVersionProvider
import com.tangem.tap.network.auth.DefaultAuthProvider
import com.tangem.tap.network.auth.DefaultExpressAuthProvider
import com.tangem.tap.network.auth.DefaultStakeKitAuthProvider
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
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): AuthProvider {
        return DefaultAuthProvider(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewListRepository = hotWalletFeatureToggles.isHotWalletEnabled,
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
    fun provideAppVersionProvider(): AppVersionProvider {
        return DefaultAppVersionProvider()
    }
}