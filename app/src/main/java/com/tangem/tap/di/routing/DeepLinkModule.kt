package com.tangem.tap.di.routing

import com.tangem.common.routing.deeplink.DeepLinkHandler
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

@Module
@InstallIn(SingletonComponent::class)
internal interface DeepLinkModule {

    /** Declares the map features contribute their [DeepLinkHandler]s to, so it may also be empty. */
    @Multibinds
    fun deepLinkHandlers(): Map<String, DeepLinkHandler>
}