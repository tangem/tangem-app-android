package com.tangem.tap.di.domain

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
    fun provideAddCryptoPortfolioUseCase(): AddCryptoPortfolioUseCase {
        return AddCryptoPortfolioUseCase()
    }

    @Provides
    @Singleton
    fun provideUpdateCryptoPortfolioUseCase(): UpdateCryptoPortfolioUseCase {
        return UpdateCryptoPortfolioUseCase()
    }

    @Provides
    @Singleton
    fun provideArchiveCryptoPortfolioUseCase(): ArchiveCryptoPortfolioUseCase {
        return ArchiveCryptoPortfolioUseCase()
    }

    @Provides
    @Singleton
    fun provideRecoverCryptoPortfolioUseCase(): RecoverCryptoPortfolioUseCase {
        return RecoverCryptoPortfolioUseCase()
    }
}