package com.tangem.features.marketing.impl.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.features.marketing.api.promopage.PromoPageDeepLinkHandler
import com.tangem.features.marketing.impl.promopage.DefaultPromoPageDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface MarketingDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(PromoPageDeepLinkHandler.DEEPLINK_KEY)
    fun bindPromoPageDeepLinkHandler(impl: DefaultPromoPageDeepLinkHandler): DeepLinkHandler
}