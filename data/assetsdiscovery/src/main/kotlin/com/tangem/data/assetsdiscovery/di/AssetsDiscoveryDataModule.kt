package com.tangem.data.assetsdiscovery.di

import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.assetsdiscovery.DefaultAssetsDiscoveryFacade
import com.tangem.data.assetsdiscovery.repository.DefaultAssetsDiscoveryRepository
import com.tangem.data.assetsdiscovery.store.AssetsDiscoveryStoreFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.assetsdiscovery.AssetsDiscoveryFacade
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AssetsDiscoveryDataModule {

    @Provides
    @Singleton
    fun provideAssetsDiscoveryFacade(
        blockchainSDKFactory: BlockchainSDKFactory,
        userWalletsListRepository: UserWalletsListRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): AssetsDiscoveryFacade = DefaultAssetsDiscoveryFacade(
        blockchainSDKFactory = blockchainSDKFactory,
        userWalletsListRepository = userWalletsListRepository,
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun provideAssetsDiscoveryRepository(
        assetsDiscoveryFacade: AssetsDiscoveryFacade,
        tangemTechApi: TangemTechApi,
        userWalletsListRepository: UserWalletsListRepository,
        networkFactory: NetworkFactory,
        appPreferencesStore: AppPreferencesStore,
        assetsDiscoveryStoreFactory: AssetsDiscoveryStoreFactory,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): AssetsDiscoveryRepository = DefaultAssetsDiscoveryRepository(
        assetsDiscoveryFacade = assetsDiscoveryFacade,
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = userWalletsListRepository,
        networkFactory = networkFactory,
        appPreferencesStore = appPreferencesStore,
        assetsDiscoveryStoreFactory = assetsDiscoveryStoreFactory,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        dispatchers = dispatchers,
        excludedBlockchains = excludedBlockchains,
    )
}