package com.tangem.tap.di.domain

import com.tangem.domain.card.*
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.IsDemoCardUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object CardDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetBiometricsStatusUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): GetBiometricsStatusUseCase {
        return GetBiometricsStatusUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSetAccessCodeRequestPolicyUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): SetAccessCodeRequestPolicyUseCase {
        return SetAccessCodeRequestPolicyUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetAccessCodeSavingStatusUseCase(
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): GetAccessCodeSavingStatusUseCase {
        return GetAccessCodeSavingStatusUseCase(cardSdkConfigRepository = cardSdkConfigRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCardWasScannedUseCase(cardRepository: CardRepository): GetCardWasScannedUseCase {
        return GetCardWasScannedUseCase(cardRepository = cardRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsDemoCardUseCase(): IsDemoCardUseCase = IsDemoCardUseCase(config = DemoConfig())
}