package com.tangem.tap.di.domain

import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingRepository
import com.tangem.domain.marketing.WarmUpMarketingCampaignsUseCase
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
    fun provideDismissMarketingBannerUseCase(repository: MarketingRepository): DismissMarketingBannerUseCase =
        DismissMarketingBannerUseCase(repository)

    @Provides
    @Singleton
    fun provideWarmUpMarketingCampaignsUseCase(
        repository: MarketingRepository,
        featureToggles: MarketingFeatureToggles,
    ): WarmUpMarketingCampaignsUseCase = WarmUpMarketingCampaignsUseCase(repository, featureToggles)
}