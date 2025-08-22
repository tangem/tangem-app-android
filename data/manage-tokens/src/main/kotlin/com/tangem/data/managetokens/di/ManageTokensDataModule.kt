package com.tangem.data.managetokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CardCryptoCurrencyFactory
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.managetokens.DefaultCustomTokensRepository
import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.token.UserTokensResponseStore
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
        userTokensResponseStore: UserTokensResponseStore,
        userTokensSaver: UserTokensSaver,
        testnetTokensStorage: TestnetTokensStorage,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        networkFactory: NetworkFactory,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            manageTokensUpdateFetcher = manageTokensUpdateFetcher,
            userTokensResponseStore = userTokensResponseStore,
            userTokenSaver = userTokensSaver,
            testnetTokensStorage = testnetTokensStorage,
            excludedBlockchains = excludedBlockchains,
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
            networkFactory = networkFactory,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideCustomTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        userTokensResponseStore: UserTokensResponseStore,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        userTokensSaver: UserTokensSaver,
        networkFactory: NetworkFactory,
    ): CustomTokensRepository {
        return DefaultCustomTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsStore = userWalletsStore,
            userTokensResponseStore = userTokensResponseStore,
            walletManagersFacade = walletManagersFacade,
            excludedBlockchains = excludedBlockchains,
            dispatchers = dispatchers,
            userTokensSaver = userTokensSaver,
            networkFactory = networkFactory,
        )
    }
}