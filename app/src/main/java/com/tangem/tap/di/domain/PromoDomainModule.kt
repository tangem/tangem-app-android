package com.tangem.tap.di.domain

import com.tangem.domain.promo.GetStoryContentUseCase
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.ShouldShowPromoTokenUseCase
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.ShouldShowStoriesUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.features.promobanners.api.NewPromoBannersFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PromoDomainModule {

    @Provides
    @Singleton
    fun provideShouldShowSwapPromoWalletUseCase(
        promoRepository: PromoRepository,
        settingsRepository: SettingsRepository,
        newPromoBannersFeatureToggles: NewPromoBannersFeatureToggles,
    ): ShouldShowPromoWalletUseCase {
        return ShouldShowPromoWalletUseCase(
            promoRepository,
            settingsRepository,
            newPromoBannersFeatureToggles.isNewPromoBannersEnabled,
        )
    }

    @Provides
    @Singleton
    fun provideShouldShowSwapPromoTokenUseCase(promoRepository: PromoRepository): ShouldShowPromoTokenUseCase {
        return ShouldShowPromoTokenUseCase(promoRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowSwapStoriesUseCase(promoRepository: PromoRepository): ShouldShowStoriesUseCase {
        return ShouldShowStoriesUseCase(promoRepository)
    }

    @Provides
    @Singleton
    fun provideGetStoryContentUseCase(
        promoRepository: PromoRepository,
        settingsRepository: SettingsRepository,
    ): GetStoryContentUseCase {
        return GetStoryContentUseCase(promoRepository, settingsRepository)
    }
}