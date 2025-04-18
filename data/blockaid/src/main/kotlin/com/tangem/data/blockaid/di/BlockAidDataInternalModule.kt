package com.tangem.data.blockaid.di

import com.tangem.data.blockaid.BlockAidRepository
import com.tangem.data.blockaid.DefaultBlockAidRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BlockAidDataInternalModule {

    @Binds
    @Singleton
    fun bindRepository(repository: DefaultBlockAidRepository): BlockAidRepository
}