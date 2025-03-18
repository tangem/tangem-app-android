package com.tangem.tap.di.domain

import com.tangem.domain.nft.FetchNFTCollectionsUseCase
import com.tangem.domain.nft.GetNFTCollectionsUseCase
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
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
    ): GetNFTCollectionsUseCase {
        return GetNFTCollectionsUseCase(
            currenciesRepository = currenciesRepository,
            nftRepository = nftRepository,
        )
    }

    @Provides
    @Singleton
    fun providesFetchNFTCollectionsUseCase(
        currenciesRepository: CurrenciesRepository,
        nftRepository: NFTRepository,
    ): FetchNFTCollectionsUseCase {
        return FetchNFTCollectionsUseCase(
            currenciesRepository = currenciesRepository,
            nftRepository = nftRepository,
        )
    }
}