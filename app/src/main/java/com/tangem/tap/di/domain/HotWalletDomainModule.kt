package com.tangem.tap.di.domain

import com.tangem.domain.hotwallet.GetAccessCodeSkippedUseCase
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.hotwallet.IsAccessCodeSimpleUseCase
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HotWalletDomainModule {

    @Provides
    @Singleton
    fun provideGetAccessCodeSkippedUseCase(hotWalletRepository: HotWalletRepository): GetAccessCodeSkippedUseCase {
        return GetAccessCodeSkippedUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideSetAccessCodeSkippedUseCase(hotWalletRepository: HotWalletRepository): SetAccessCodeSkippedUseCase {
        return SetAccessCodeSkippedUseCase(hotWalletRepository)
    }

    @Provides
    @Singleton
    fun provideIsAccessCodeSimpleUseCase(): IsAccessCodeSimpleUseCase {
        return IsAccessCodeSimpleUseCase()
    }

    @Provides
    @Singleton
    fun provideIsWalletCreationSupportedUseCase(
        hotWalletRepository: HotWalletRepository,
    ): IsHotWalletCreationSupported {
        return IsHotWalletCreationSupported(hotWalletRepository)
    }
}