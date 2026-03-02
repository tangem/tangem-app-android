package com.tangem.data.managetokens.di

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.managetokens.DefaultCustomTokensRepository
import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.managetokens.repository.ManageTokensRepository
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
        userWalletsListRepository: UserWalletsListRepository,
        manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
        userTokensResponseStore: UserTokensResponseStore,
        testnetTokensStorage: TestnetTokensStorage,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        networkFactory: NetworkFactory,
        walletAccountsFetcher: WalletAccountsFetcher,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            manageTokensUpdateFetcher = manageTokensUpdateFetcher,
            userTokensResponseStore = userTokensResponseStore,
            testnetTokensStorage = testnetTokensStorage,
            excludedBlockchains = excludedBlockchains,
            networkFactory = networkFactory,
            dispatchers = dispatchers,
            walletAccountsFetcher = walletAccountsFetcher,
        )
    }

    @Provides
    @Singleton
    fun provideCustomTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        userTokensResponseStore: UserTokensResponseStore,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        networkFactory: NetworkFactory,
    ): CustomTokensRepository {
        return DefaultCustomTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            userTokensResponseStore = userTokensResponseStore,
            excludedBlockchains = excludedBlockchains,
            dispatchers = dispatchers,
            networkFactory = networkFactory,
        )
    }
}