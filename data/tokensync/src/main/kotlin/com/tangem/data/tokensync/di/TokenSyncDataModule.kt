package com.tangem.data.tokensync.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.tokensync.repository.DefaultTokenSyncRepository
import com.tangem.data.tokensync.store.TokenSyncStoreFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.tokensync.repository.TokenSyncRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokenSyncDataModule {

    @Provides
    @Singleton
    fun provideTokenSyncRepository(
        walletManagersFacade: WalletManagersFacade,
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        networkFactory: NetworkFactory,
        appPreferencesStore: AppPreferencesStore,
        tokenSyncStoreFactory: TokenSyncStoreFactory,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): TokenSyncRepository {
        return DefaultTokenSyncRepository(
            walletManagersFacade = walletManagersFacade,
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            networkFactory = networkFactory,
            appPreferencesStore = appPreferencesStore,
            tokenSyncStoreFactory = tokenSyncStoreFactory,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
        )
    }
}