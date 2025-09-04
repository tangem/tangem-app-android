package com.tangem.tap.di.domain

import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.nft.*
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.repository.WalletsRepository
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
        nftRepository: NFTRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): FetchNFTCollectionsUseCase = FetchNFTCollectionsUseCase(
        nftRepository = nftRepository,
        multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
    )

    @Provides
    @Singleton
    fun providesRefreshAllNFTUseCase(
        nftRepository: NFTRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): RefreshAllNFTUseCase = RefreshAllNFTUseCase(
        nftRepository = nftRepository,
        multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
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
        singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    ): GetNFTPriceUseCase {
        return GetNFTPriceUseCase(nftRepository, singleQuoteStatusSupplier)
    }

    @Provides
    @Singleton
    fun provideFetchNFTPriceUseCase(
        nftRepository: NFTRepository,
        singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    ): FetchNFTPriceUseCase {
        return FetchNFTPriceUseCase(nftRepository, singleQuoteStatusFetcher)
    }

    @Provides
    @Singleton
    fun provideEnableWalletNFTUseCase(walletsRepository: WalletsRepository): EnableWalletNFTUseCase {
        return EnableWalletNFTUseCase(walletsRepository)
    }

    @Provides
    @Singleton
    fun provideDisableWalletNFTUseCase(
        walletsRepository: WalletsRepository,
        nftRepository: NFTRepository,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    ): DisableWalletNFTUseCase {
        return DisableWalletNFTUseCase(
            walletsRepository = walletsRepository,
            nftRepository = nftRepository,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        )
    }

    @Provides
    @Singleton
    fun provideGetWalletNFTEnabledUseCase(walletsRepository: WalletsRepository): GetWalletNFTEnabledUseCase {
        return GetWalletNFTEnabledUseCase(walletsRepository)
    }

    @Provides
    @Singleton
    fun provideClearNFTCacheUseCase(
        nftRepository: NFTRepository,
        currenciesRepository: CurrenciesRepository,
    ): ObserveAndClearNFTCacheIfNeedUseCase {
        return ObserveAndClearNFTCacheIfNeedUseCase(nftRepository, currenciesRepository)
    }

    @Provides
    @Singleton
    fun provideGetNftCurrencyUseCase(nftRepository: NFTRepository): GetNFTCurrencyUseCase {
        return GetNFTCurrencyUseCase(nftRepository)
    }
}