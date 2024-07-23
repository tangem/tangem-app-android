package com.tangem.datasource.di

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.AppPreferencesUserTokensStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.token.UserTokensStoreMigrationRunner
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UserTokensStoreModule {

    @Provides
    @Singleton
    fun provideUserTokensStore(
        appPreferencesStore: AppPreferencesStore,
        userTokensStoreMigrationRunner: UserTokensStoreMigrationRunner,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): UserTokensStore {
        return AppPreferencesUserTokensStore(
            appPreferencesStore = appPreferencesStore,
            userTokensStoreMigrationRunner = userTokensStoreMigrationRunner,
            userWalletsStore = userWalletsStore,
            dispatchers = dispatchers,
        )
    }
}
