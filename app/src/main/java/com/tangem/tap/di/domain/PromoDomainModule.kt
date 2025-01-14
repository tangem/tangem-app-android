package com.tangem.tap.di.domain

import com.tangem.domain.promo.*
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
        promoSettingsRepository: PromoRepository,
    ): ShouldShowSwapPromoWalletUseCase {
        return ShouldShowSwapPromoWalletUseCase(promoSettingsRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowRingPromoUseCase(promoRepository: PromoRepository): ShouldShowRingPromoUseCase {
        return ShouldShowRingPromoUseCase(promoRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowSwapPromoTokenUseCase(promoRepository: PromoRepository): ShouldShowSwapPromoTokenUseCase {
        return ShouldShowSwapPromoTokenUseCase(promoRepository)
    }

    @Provides
    @Singleton
    fun provideShouldShowSwapStoriesUseCase(promoRepository: PromoRepository): ShouldShowSwapStoriesUseCase {
        return ShouldShowSwapStoriesUseCase(promoRepository)
    }
}