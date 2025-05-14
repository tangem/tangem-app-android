package com.tangem.tap.di.domain

import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.nft.*
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.quotes.single.SingleQuoteFetcher
import com.tangem.domain.quotes.single.SingleQuoteSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
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
    fun providesRefreshAllNFTUseCase(
        currenciesRepository: CurrenciesRepository,
        nftRepository: NFTRepository,
    ): RefreshAllNFTUseCase = RefreshAllNFTUseCase(
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
    ): GetNFTNetworksUseCase = GetNFTNetworksUseCase(
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
    fun providesGetNFTNetworkStatusUseCase(
        singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    ): GetNFTNetworkStatusUseCase {
        return GetNFTNetworkStatusUseCase(singleNetworkStatusSupplier = singleNetworkStatusSupplier)
    }

    @Provides
    @Singleton
    fun providesGetNFTExploreUrlUseCase(nftRepository: NFTRepository): GetNFTExploreUrlUseCase =
        GetNFTExploreUrlUseCase(
            nftRepository = nftRepository,
        )

    @Provides
    @Singleton
    fun provideGetNFTPriceUseCase(
        nftRepository: NFTRepository,
        singleQuoteSupplier: SingleQuoteSupplier,
    ): GetNFTPriceUseCase {
        return GetNFTPriceUseCase(nftRepository, singleQuoteSupplier)
    }

    @Provides
    @Singleton
    fun provideFetchNFTPriceUseCase(
        nftRepository: NFTRepository,
        singleQuoteFetcher: SingleQuoteFetcher,
    ): FetchNFTPriceUseCase {
        return FetchNFTPriceUseCase(nftRepository, singleQuoteFetcher)
    }
}