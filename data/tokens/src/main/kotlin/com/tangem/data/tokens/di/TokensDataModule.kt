package com.tangem.data.tokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.tokens.repository.DefaultCurrenciesRepository
import com.tangem.data.tokens.repository.DefaultCurrencyChecksRepository
import com.tangem.data.tokens.repository.DefaultTokenReceiveWarningsViewedRepository
import com.tangem.data.tokens.repository.DefaultYieldSupplyWarningsViewedRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.TokenReceiveWarningActionStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.repository.TokenReceiveWarningsViewedRepository
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository
import com.tangem.domain.transaction.MemoValidatorFacade
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TokensDataModule {

    @Provides
    @Singleton
    fun provideCurrenciesRepository(
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): CurrenciesRepository {
        return DefaultCurrenciesRepository(
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    fun provideCurrencyChecksRepository(
        walletManagersFacade: WalletManagersFacade,
        memoValidatorFacade: MemoValidatorFacade,
        coroutineDispatcherProvider: CoroutineDispatcherProvider,
    ): CurrencyChecksRepository {
        return DefaultCurrencyChecksRepository(
            walletManagersFacade = walletManagersFacade,
            memoValidatorFacade = memoValidatorFacade,
            coroutineDispatchers = coroutineDispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun provideTokenReceiveWarningsViewedRepository(
        tokenReceiveWarningActionStore: TokenReceiveWarningActionStore,
    ): TokenReceiveWarningsViewedRepository {
        return DefaultTokenReceiveWarningsViewedRepository(
            tokenReceiveWarningActionStore = tokenReceiveWarningActionStore,
        )
    }

    @Provides
    @Singleton
    fun provideDefaultYieldSupplyWarningsViewedRepository(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldSupplyWarningsViewedRepository {
        return DefaultYieldSupplyWarningsViewedRepository(
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }
}