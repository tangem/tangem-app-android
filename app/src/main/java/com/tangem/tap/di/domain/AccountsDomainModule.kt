package com.tangem.tap.di.domain

import com.tangem.domain.accounts.*
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountsDomainModule {

    @Provides
    @Singleton
    fun provideGetAccountsUseCase(repository: CryptoCurrenciesAccountsRepository): GetAccountsUseCase {
        return GetAccountsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetSelectedAccountUseCase(repository: CryptoCurrenciesAccountsRepository): GetSelectedAccountIdUseCase {
        return GetSelectedAccountIdUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateAccountUseCase(repository: CryptoCurrenciesAccountsRepository): UpdateAccountUseCase {
        return UpdateAccountUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSelectAccountUseCase(repository: CryptoCurrenciesAccountsRepository): SelectAccountUseCase {
        return SelectAccountUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCreateAccountUseCase(repository: CryptoCurrenciesAccountsRepository): CreateAccountUseCase {
        return CreateAccountUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCheckIsValidAccountUseCase(repository: CryptoCurrenciesAccountsRepository): CheckIsValidAccountUseCase {
        return CheckIsValidAccountUseCase(repository)
    }
}