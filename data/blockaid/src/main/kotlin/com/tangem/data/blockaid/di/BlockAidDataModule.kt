package com.tangem.data.blockaid.di

import com.tangem.data.blockaid.DefaultBlockAidVerifier
import com.tangem.domain.blockaid.BlockAidVerifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BlockAidDataModule {

    @Binds
    @Singleton
    fun bindVerifier(verifier: DefaultBlockAidVerifier): BlockAidVerifier
}