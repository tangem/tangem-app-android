package com.tangem.feature.usedesk.deeplink.di

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkKey
import com.tangem.feature.usedesk.deeplink.SupportDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SupportDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(SupportDeepLinkHandler.DEEPLINK_KEY)
    fun bindSupportDeepLinkHandler(impl: SupportDeepLinkHandler): DeepLinkHandler
}