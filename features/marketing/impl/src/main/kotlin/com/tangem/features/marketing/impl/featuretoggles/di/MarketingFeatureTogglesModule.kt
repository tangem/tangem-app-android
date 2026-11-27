package com.tangem.features.marketing.impl.featuretoggles.di

import com.tangem.features.marketing.api.MarketingFeatureToggles
import com.tangem.features.marketing.impl.featuretoggles.DefaultMarketingFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MarketingFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindMarketingFeatureToggles(impl: DefaultMarketingFeatureToggles): MarketingFeatureToggles
}