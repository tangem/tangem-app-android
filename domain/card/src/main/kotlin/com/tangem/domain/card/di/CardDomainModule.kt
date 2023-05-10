package com.tangem.domain.card.di

import com.tangem.TangemSdk
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.ScanCardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class CardDomainModule {

    @Provides
    fun provideScanCardUseCase(scanCardRepository: ScanCardRepository, tangemSdk: TangemSdk) =
        ScanCardUseCase(scanCardRepository, tangemSdk)
}