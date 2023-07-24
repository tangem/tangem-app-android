package com.tangem.tap.di.domain

import com.tangem.domain.card.*
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.tap.domain.scanCard.DefaultScanCardProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDomainModule {

    @Provides
    @Singleton
    fun provideScanCardUseCase(): ScanCardProcessor = DefaultScanCardProcessor()

    @Provides
    @Singleton
    fun provideGetBiometricsStatusUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): GetBiometricsStatusUseCase {
        return GetBiometricsStatusUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @Singleton
    fun provideSetAccessCodeRequestPolicyUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): SetAccessCodeRequestPolicyUseCase {
        return SetAccessCodeRequestPolicyUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @Singleton
    fun provideGetAccessCodeSavingStatusUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): GetAccessCodeSavingStatusUseCase {
        return GetAccessCodeSavingStatusUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @Singleton
    fun provideGetCardWasScannedUseCase(cardRepository: CardRepository): GetCardWasScannedUseCase {
        return GetCardWasScannedUseCase(cardRepository = cardRepository)
    }
}
