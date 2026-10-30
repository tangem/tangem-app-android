package com.tangem.features.marketing.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.domain.marketing.MarketingFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal object MarketingDeepLinkModule {

    // Only makes the link known to the uid binding; PromoPageRouter opens the page
    @Provides
    @IntoMap
    @DeepLinkKey("tangem://promo_page")
    fun providePromoPageDeepLinkHandler(marketingFeatureToggles: MarketingFeatureToggles): DeepLinkHandler =
        DeepLinkHandler(isEnabled = { marketingFeatureToggles.isPromoPageEnabled }, handle = {})
}