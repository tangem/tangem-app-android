package com.tangem.tap.network.auth.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.tap.network.auth.DefaultAppVersionProvider
import com.tangem.tap.network.auth.DefaultAuthProvider
import com.tangem.tap.network.auth.DefaultExpressAuthProvider
import com.tangem.tap.network.auth.DefaultStakeKitAuthProvider
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.version.AppVersionProvider
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
        return DefaultAuthProvider(appStateHolder)
    }

    @Provides
    @Singleton
    fun provideExpressAuthProvider(userWalletsStore: UserWalletsStore): ExpressAuthProvider {
        return DefaultExpressAuthProvider(userWalletsStore = userWalletsStore)
    }

    @Provides
    @Singleton
    fun provideStakeKitAuthProvider(configManager: ConfigManager): StakeKitAuthProvider {
        return DefaultStakeKitAuthProvider(configManager)
    }

    @Provides
    @Singleton
    fun provideAppVersionProvider(): AppVersionProvider {
        return DefaultAppVersionProvider()
    }
}
