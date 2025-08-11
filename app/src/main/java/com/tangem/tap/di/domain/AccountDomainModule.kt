package com.tangem.tap.di.domain

import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.usecase.AddCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.ArchiveCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.RecoverCryptoPortfolioUseCase
import com.tangem.domain.account.usecase.UpdateCryptoPortfolioUseCase
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
    fun provideRecoverCryptoPortfolioUseCase(): RecoverCryptoPortfolioUseCase {
        return RecoverCryptoPortfolioUseCase()
    }
}