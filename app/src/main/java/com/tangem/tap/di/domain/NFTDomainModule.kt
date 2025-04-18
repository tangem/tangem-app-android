package com.tangem.tap.di.domain

import com.tangem.domain.nft.*
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NFTDomainModule {

    @Provides
    @Singleton
    fun providesGetNFTCollectionsUseCase(
        currenciesRepository: CurrenciesRepository,
        nftRepository: NFTRepository,
    ): GetNFTCollectionsUseCase = GetNFTCollectionsUseCase(
        currenciesRepository = currenciesRepository,
        nftRepository = nftRepository,
    )

    @Provides
    @Singleton
    fun providesFetchNFTCollectionsUseCase(
        currenciesRepository: CurrenciesRepository,
        nftRepository: NFTRepository,
    ): FetchNFTCollectionsUseCase = FetchNFTCollectionsUseCase(
        currenciesRepository = currenciesRepository,
        nftRepository = nftRepository,
    )

    @Provides
    @Singleton
    fun providesFetchNFTCollectionAssetsUseCase(nftRepository: NFTRepository): FetchNFTCollectionAssetsUseCase =
        FetchNFTCollectionAssetsUseCase(
            nftRepository = nftRepository,
        )

    @Provides
    @Singleton
    fun providesGetNFTAvailableNetworksUseCase(
        nftRepository: NFTRepository,
        currenciesRepository: CurrenciesRepository,
    ): GetNFTAvailableNetworksUseCase = GetNFTAvailableNetworksUseCase(
        currenciesRepository = currenciesRepository,
        nftRepository = nftRepository,
    )

    @Provides
    @Singleton
    fun providesFilterNFTAvailableNetworksUseCase(
        dispatchers: CoroutineDispatcherProvider,
    ): FilterNFTAvailableNetworksUseCase = FilterNFTAvailableNetworksUseCase(
        dispatchers = dispatchers,
    )

    @Provides
    @Singleton
    fun providesGetNFTNetworkStatusUseCase(networksRepository: NetworksRepository): GetNFTNetworkStatusUseCase =
        GetNFTNetworkStatusUseCase(
            networksRepository = networksRepository,
        )

    @Provides
    @Singleton
    fun providesGetNFTExploreUrlUseCase(nftRepository: NFTRepository): GetNFTExploreUrlUseCase =
        GetNFTExploreUrlUseCase(
            nftRepository = nftRepository,
        )
}