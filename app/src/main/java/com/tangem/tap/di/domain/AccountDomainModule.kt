package com.tangem.tap.di.domain

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDomainModule {

    @Provides
    @Singleton
    fun provideAddCryptoPortfolioUseCase(accountsCRUDRepository: AccountsCRUDRepository): AddCryptoPortfolioUseCase {
        return AddCryptoPortfolioUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): UpdateCryptoPortfolioUseCase {
        return UpdateCryptoPortfolioUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideArchiveCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): ArchiveCryptoPortfolioUseCase {
        return ArchiveCryptoPortfolioUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideRecoverCryptoPortfolioUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): RecoverCryptoPortfolioUseCase {
        return RecoverCryptoPortfolioUseCase(crudRepository = accountsCRUDRepository)
    }

    @Provides
    @Singleton
    fun provideGetUnoccupiedAccountIndexUseCase(
        accountsCRUDRepository: AccountsCRUDRepository,
    ): GetUnoccupiedAccountIndexUseCase {
        return GetUnoccupiedAccountIndexUseCase(crudRepository = accountsCRUDRepository)
    }
}