package com.tangem.data.managetokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.managetokens.DefaultCustomTokensRepository
import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ManageTokensDataModule {

    @Provides
    @Singleton
    fun provideManageTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
        appPreferencesStore: AppPreferencesStore,
        testnetTokensStorage: TestnetTokensStorage,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            manageTokensUpdateFetcher = manageTokensUpdateFetcher,
            appPreferencesStore = appPreferencesStore,
            testnetTokensStorage = testnetTokensStorage,
            excludedBlockchains = excludedBlockchains,
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideCustomTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        appPreferencesStore: AppPreferencesStore,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        userTokensSaver: UserTokensSaver,
    ): CustomTokensRepository {
        return DefaultCustomTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            appPreferencesStore = appPreferencesStore,
            walletManagersFacade = walletManagersFacade,
            excludedBlockchains = excludedBlockchains,
            dispatchers = dispatchers,
            userTokensSaver = userTokensSaver,
        )
    }
}