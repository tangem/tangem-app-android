package com.tangem.tap.di.domain

import com.tangem.domain.promo.*
import com.tangem.domain.settings.repositories.SettingsRepository
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
    ): ShouldShowPromoWalletUseCase {
        return ShouldShowPromoWalletUseCase(promoRepository, settingsRepository)
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