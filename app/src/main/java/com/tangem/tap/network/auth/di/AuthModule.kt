package com.tangem.tap.network.auth.di

import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.common.visa.TangemVisaAuthProvider
import com.tangem.datasource.local.config.environment.EnvironmentConfigStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.auth.ExpressAuthProvider
import com.tangem.lib.auth.StakeKitAuthProvider
import com.tangem.tap.network.auth.*
import com.tangem.tap.network.auth.DefaultAppVersionProvider
import com.tangem.tap.network.auth.DefaultAuthProvider
import com.tangem.tap.network.auth.DefaultExpressAuthProvider
import com.tangem.tap.network.auth.DefaultStakeKitAuthProvider
import com.tangem.tap.network.auth.DefaultVisaAuthProvider
import com.tangem.utils.version.AppVersionProvider
import dagger.Binds
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
    fun provideAuthProvider(userWalletsListManager: UserWalletsListManager): AuthProvider {
        return DefaultAuthProvider(userWalletsListManager)
    }

    @Provides
    @Singleton
    fun provideExpressAuthProvider(
        userWalletsStore: UserWalletsStore,
        appPreferencesStore: AppPreferencesStore,
    ): ExpressAuthProvider {
        return DefaultExpressAuthProvider(
            userWalletsStore = userWalletsStore,
            appPreferencesStore = appPreferencesStore,
        )
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

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthBindModule {

    @Binds
    @Singleton
    fun bindVisaAuthProvider(authStorage: DefaultVisaAuthProvider): TangemVisaAuthProvider
}