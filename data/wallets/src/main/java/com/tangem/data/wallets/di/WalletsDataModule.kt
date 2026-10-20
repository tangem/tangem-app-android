package com.tangem.data.wallets.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.data.wallet.BuildConfig
import com.tangem.data.wallets.DefaultWalletCardsBackupRepository
import com.tangem.data.wallets.DefaultWalletNamesMigrationRepository
import com.tangem.data.wallets.DefaultWalletsPromoRepository
import com.tangem.data.wallets.DefaultWalletsRepository
import com.tangem.data.wallets.cold.DefaultColdMapDerivationsRepository
import com.tangem.data.wallets.derivations.DefaultDerivationsHelper
import com.tangem.data.wallets.derivations.DefaultDerivationsRepository
import com.tangem.data.wallets.hot.DefaultHotMapDerivationsRepository
import com.tangem.data.wallets.hot.DefaultHotWalletAccessCodeAttemptsRepository
import com.tangem.data.wallets.signing.DefaultDeriveAndSignSession
import com.tangem.data.wallets.signing.DefaultInSessionKeyDeriver
import com.tangem.data.wallets.signing.InSessionKeyDeriver
import com.tangem.data.wallets.signing.MockDeriveAndSignSession
import com.tangem.data.wallets.store.PendingWalletCardsBackup
import com.tangem.data.wallets.store.PendingWalletCardsBackupStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.wallets.derivations.ColdMapDerivationsRepository
import com.tangem.domain.wallets.derivations.DerivationsHelper
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.derivations.HotMapDerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.repository.WalletCardsBackupRepository
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsPromoRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.signing.DeriveAndSignSession
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer

@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDataModule {

    @Provides
    @Singleton
    fun providesWalletsRepository(
        appPreferencesStore: AppPreferencesStore,
        storePreferencesKeys: StorePreferencesKeys,
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        dispatchers: CoroutineDispatcherProvider,
        walletServerBinder: WalletServerBinder,
        @NetworkMoshi moshi: Moshi,
    ): WalletsRepository {
        return DefaultWalletsRepository(
            appPreferencesStore = appPreferencesStore,
            storePreferencesKeys = storePreferencesKeys,
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatchers,
            walletServerBinder = walletServerBinder,
            moshi = moshi,
        )
    }

    @Provides
    @Singleton
    fun provideWalletCardsBackupRepository(
        tangemTechApi: TangemTechApi,
        dataStoreFactory: AppDataStoreFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletCardsBackupRepository {
        return DefaultWalletCardsBackupRepository(
            tangemTechApi = tangemTechApi,
            pendingStore = PendingWalletCardsBackupStore(
                dataStore = dataStoreFactory.create(
                    defaultValue = emptyList(),
                    serializer = ListSerializer(PendingWalletCardsBackup.serializer()),
                    fileName = "pending_wallet_cards_backups",
                ),
            ),
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideMigrateNamesRepository(
        appPreferencesStore: AppPreferencesStore,
        storePreferencesKeys: StorePreferencesKeys,
    ): WalletNamesMigrationRepository {
        return DefaultWalletNamesMigrationRepository(appPreferencesStore, storePreferencesKeys)
    }

    @Provides
    @Singleton
    fun provideWalletsPromoRepository(
        appPreferencesStore: AppPreferencesStore,
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        dispatchers: CoroutineDispatcherProvider,
        appsFlyerRepository: AppsFlyerRepository,
    ): WalletsPromoRepository {
        return DefaultWalletsPromoRepository(
            appPreferencesStore = appPreferencesStore,
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatchers,
            appsFlyerRepository = appsFlyerRepository,
        )
    }

    @Provides
    @Singleton
    fun provideDerivationsHelper(): DerivationsHelper {
        return DefaultDerivationsHelper(
            demoConfig = DemoConfig,
        )
    }

    @Provides
    @Singleton
    fun provideDeriveAndSignSession(
        defaultSession: Provider<DefaultDeriveAndSignSession>,
        mockSession: Provider<MockDeriveAndSignSession>,
    ): DeriveAndSignSession = if (BuildConfig.MOCK_DATA_SOURCE) mockSession.get() else defaultSession.get()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletsDataBindsModule {

    @Binds
    @Singleton
    fun bindInSessionKeyDeriver(impl: DefaultInSessionKeyDeriver): InSessionKeyDeriver

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