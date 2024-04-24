package com.tangem.data.accounts.di

import com.tangem.data.accounts.NotImplementedAccountsRepository
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountsDataModule {

    @Provides
    @Singleton
    fun provideAccountsRepository(): CryptoCurrenciesAccountsRepository {
        return NotImplementedAccountsRepository()
    }
}