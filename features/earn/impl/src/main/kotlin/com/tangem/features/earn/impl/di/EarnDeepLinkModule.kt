package com.tangem.features.earn.impl.di

import com.tangem.core.navigation.deeplink.DeepLinkHandler
import com.tangem.core.navigation.deeplink.DeepLinkKey
import com.tangem.features.earn.impl.deeplink.EarnDeepLinkHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnDeepLinkModule {

    @Binds
    @IntoMap
    @DeepLinkKey(EarnDeepLinkHandler.DEEPLINK_KEY)
    fun bindEarnDeepLinkHandler(impl: EarnDeepLinkHandler): DeepLinkHandler
}