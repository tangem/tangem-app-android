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
    ): ShouldShowPromoWalletUseCase {
        return ShouldShowPromoWalletUseCase(promoSettingsRepository)
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
    fun provideGetStoryContentUseCase(promoRepository: PromoRepository): GetStoryContentUseCase {
        return GetStoryContentUseCase(promoRepository)
    }
}