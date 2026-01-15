package com.tangem.data.wallets.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.data.wallets.DefaultWalletNamesMigrationRepository
import com.tangem.data.wallets.DefaultWalletsRepository
import com.tangem.data.wallets.cold.DefaultColdMapDerivationsRepository
import com.tangem.data.wallets.derivations.DefaultDerivationsRepository
import com.tangem.data.wallets.hot.DefaultHotMapDerivationsRepository
import com.tangem.data.wallets.hot.DefaultHotWalletAccessCodeAttemptsRepository
import com.tangem.datasource.api.common.AuthProvider
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.derivations.HotMapDerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
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
        walletServerBinder: WalletServerBinder,
        appsFlyerConversionStore: AppsFlyerConversionStore,
        accountsFeatureToggles: AccountsFeatureToggles,
        @NetworkMoshi moshi: Moshi,
    ): WalletsRepository {
        return DefaultWalletsRepository(
            appPreferencesStore = appPreferencesStore,
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            seedPhraseNotificationVisibilityStore = RuntimeStateStore(defaultValue = emptyMap()),
            dispatchers = dispatchers,
            authProvider = authProvider,
            walletServerBinder = walletServerBinder,
            appsFlyerConversionStore = appsFlyerConversionStore,
            accountsFeatureToggles = accountsFeatureToggles,
            moshi = moshi,
        )
    }

    @Provides
    @Singleton
    fun provideMigrateNamesRepository(appPreferencesStore: AppPreferencesStore): WalletNamesMigrationRepository {
        return DefaultWalletNamesMigrationRepository(appPreferencesStore)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletsDataBindsModule {

    @Binds
    @Singleton
    fun bindDerivationsRepository(impl: DefaultDerivationsRepository): DerivationsRepository

    @Binds
    @Singleton
    fun bindHotMapDerivationsRepository(impl: DefaultHotMapDerivationsRepository): HotMapDerivationsRepository

    @Binds
    @Singleton
    fun bindColdMapDerivationsRepository(impl: DefaultColdMapDerivationsRepository): ColdMapDerivationsRepository

    @Binds
    @Singleton
    fun bindHotWalletAccessCodeAttemptsRepository(
        impl: DefaultHotWalletAccessCodeAttemptsRepository,
    ): HotWalletAccessCodeAttemptsRepository
}