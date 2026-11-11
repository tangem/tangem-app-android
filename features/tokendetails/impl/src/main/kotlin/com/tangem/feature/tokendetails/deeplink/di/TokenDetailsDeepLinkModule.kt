package com.tangem.feature.tokendetails.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.tokendetails.deeplink.DefaultExpressDeepLinkTrigger
import com.tangem.feature.tokendetails.deeplink.DefaultTokenDetailsDeepLinkHandler
import com.tangem.feature.tokendetails.deeplink.TokenDetailsDeepLinkActionTrigger
import com.tangem.features.tokendetails.deeplink.ExpressDeepLinkListener
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenDetailsDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(TokenDetailsDeepLinkHandler.DEEPLINK_KEY)
    fun bindTokenDetailsDeepLinkHandler(impl: DefaultTokenDetailsDeepLinkHandler): DeepLinkHandler

    @Binds
    @Singleton
    fun bindTokenDetailsDeepLinkActionTrigger(impl: DefaultExpressDeepLinkTrigger): TokenDetailsDeepLinkActionTrigger

    @Binds
    @Singleton
    fun bindTokenDetailsDeepLinkActionListener(impl: DefaultExpressDeepLinkTrigger): ExpressDeepLinkListener
}