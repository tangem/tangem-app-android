package com.tangem.tap.di.routing

import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.tap.routing.utils.DeepLinkFeatureToggles
import com.tangem.tap.routing.utils.DefaultDeepLinkFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DeepLinkModule {

    /** Declares the map features contribute their [DeepLinkHandler]s to, so it may also be empty. */
    @Multibinds
    fun deepLinkHandlers(): Map<String, DeepLinkHandler>

    companion object {

        @Provides
        @Singleton
        fun provideDeepLinkFeatureToggles(featureTogglesManager: FeatureTogglesManager): DeepLinkFeatureToggles =
            DefaultDeepLinkFeatureToggles(featureTogglesManager)
    }
}