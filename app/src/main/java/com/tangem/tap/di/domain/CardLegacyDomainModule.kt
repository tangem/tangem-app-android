package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.usecase.GenerateWalletNameUseCase
import com.tangem.tap.domain.scanCard.CardScanningFeatureToggles
import com.tangem.tap.domain.scanCard.DefaultScanCardProcessor
import com.tangem.tap.domain.scanCard.LegacyScanProcessor
import com.tangem.tap.domain.scanCard.UseCaseScanProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardLegacyDomainModule {

    @Provides
    @Singleton
    fun provideCardScanningFeatureToggles(featureTogglesManager: FeatureTogglesManager): CardScanningFeatureToggles {
        return CardScanningFeatureToggles(featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideScanCardProcessor(
        legacyScanProcessor: LegacyScanProcessor,
        useCaseScanProcessor: UseCaseScanProcessor,
        cardScanningFeatureToggles: CardScanningFeatureToggles,
    ): ScanCardProcessor {
        return DefaultScanCardProcessor(
            legacyScanProcessor = legacyScanProcessor,
            useCaseScanProcessor = useCaseScanProcessor,
            cardScanningFeatureToggles = cardScanningFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun providesWalletNameGenerateUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GenerateWalletNameUseCase {
        return GenerateWalletNameUseCase(userWalletsListRepository = userWalletsListRepository)
    }
}