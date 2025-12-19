package com.tangem.data.nft.di

import com.tangem.data.nft.DefaultNFTRepository
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.nft.utils.NFTCleaner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NFTDataModule {

    @Binds
    @Singleton
    fun bindNFTRepository(defaultNFTRepository: DefaultNFTRepository): NFTRepository

    @Binds
    @Singleton
    fun bindNFTCleaner(defaultNFTRepository: DefaultNFTRepository): NFTCleaner
}