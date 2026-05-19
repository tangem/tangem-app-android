package com.tangem.data.account.di

import com.tangem.data.account.repository.DefaultAccountsExpandedRepository
import com.tangem.domain.account.repository.AccountsExpandedRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountUtilsModule {

    @Binds
    fun provideAccountsExpandedRepositoryFactory(
        factory: DefaultAccountsExpandedRepository.Factory,
    ): AccountsExpandedRepository.Factory
}