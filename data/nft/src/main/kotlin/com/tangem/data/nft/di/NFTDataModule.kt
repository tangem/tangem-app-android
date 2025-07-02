package com.tangem.data.nft.di

import android.content.Context
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.nft.DefaultNFTRepository
import com.tangem.datasource.local.nft.NFTPersistenceStoreFactory
import com.tangem.datasource.local.nft.NFTRuntimeStoreFactory
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.features.nft.NFTFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NFTDataModule {

    @Provides
    @Singleton
    fun provideNFTRepository(
        @ApplicationContext context: Context,
        nftPersistenceStoreFactory: NFTPersistenceStoreFactory,
        nftRuntimeStoreFactory: NFTRuntimeStoreFactory,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
        userWalletsStore: UserWalletsStore,
        nftFeatureToggles: NFTFeatureToggles,
        networkFactory: NetworkFactory,
    ): NFTRepository = DefaultNFTRepository(
        nftPersistenceStoreFactory = nftPersistenceStoreFactory,
        nftRuntimeStoreFactory = nftRuntimeStoreFactory,
        walletManagersFacade = walletManagersFacade,
        dispatchers = dispatchers,
        excludedBlockchains = excludedBlockchains,
        userWalletsStore = userWalletsStore,
        nftFeatureToggles = nftFeatureToggles,
        networkFactory = networkFactory,
        resources = context.resources,
    )
}