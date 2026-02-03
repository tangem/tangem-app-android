package com.tangem.features.hotwallet.di

import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.common.repository.DefaultMnemonicRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MnemonicRepositoryModule {

    @Binds
    @Singleton
    fun provideMnemonicRepository(repository: DefaultMnemonicRepository): MnemonicRepository
}