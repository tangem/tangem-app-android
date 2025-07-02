package com.tangem.data.wallets.di

import com.tangem.data.wallets.DefaultWalletNamesMigrationRepository
import com.tangem.data.wallets.DefaultWalletsRepository
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDataModule {

    @Provides
    @Singleton
    fun providesWalletsRepository(
        appPreferencesStore: AppPreferencesStore,
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        dispatchers: CoroutineDispatcherProvider,
        authProvider: AuthProvider,
    ): WalletsRepository {
        return DefaultWalletsRepository(
            appPreferencesStore = appPreferencesStore,
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            seedPhraseNotificationVisibilityStore = RuntimeStateStore(defaultValue = emptyMap()),
            dispatchers = dispatchers,
            authProvider = authProvider,
        )
    }

    @Provides
    @Singleton
    fun provideMigrateNamesRepository(appPreferencesStore: AppPreferencesStore): WalletNamesMigrationRepository {
        return DefaultWalletNamesMigrationRepository(appPreferencesStore)
    }
}