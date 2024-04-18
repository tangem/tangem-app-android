package com.tangem.tap.di.domain

import com.tangem.domain.card.*
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.card.repository.DerivationsRepository
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
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
    fun provideWasWalletAlreadySignedHashesConfirmedUseCase(cardRepository: CardRepository): WasCardScannedUseCase {
        return WasCardScannedUseCase(cardRepository = cardRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideSetCardWasScannedUseCase(cardRepository: CardRepository): SetCardWasScannedUseCase {
        return SetCardWasScannedUseCase(cardRepository = cardRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsDemoCardUseCase(): IsDemoCardUseCase = IsDemoCardUseCase(config = DemoConfig())

    @Provides
    @ViewModelScoped
    fun provideDerivePublicKeysUseCase(derivationsRepository: DerivationsRepository): DerivePublicKeysUseCase {
        return DerivePublicKeysUseCase(derivationsRepository = derivationsRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideIsNeedToBackupUseCase(userWalletsListManager: UserWalletsListManager): IsNeedToBackupUseCase {
        return IsNeedToBackupUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun provideGetExtendedPublicKeyForCurrencyUseCase(
        derivationsRepository: DerivationsRepository,
    ): GetExtendedPublicKeyForCurrencyUseCase {
        return GetExtendedPublicKeyForCurrencyUseCase(derivationsRepository)
    }
}