package com.tangem.tap.di.domain

import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MarketingDomainModule {

    @Provides
    @Singleton
    fun provideGetMarketingBannerUseCase(
        repository: MarketingRepository,
        featureToggles: MarketingFeatureToggles,
    ): GetMarketingBannerUseCase = GetMarketingBannerUseCase(repository, featureToggles)

    @Provides
    @Singleton
    fun provideDismissMarketingBannerUseCase(
        repository: MarketingRepository,
    ): DismissMarketingBannerUseCase = DismissMarketingBannerUseCase(repository)
}