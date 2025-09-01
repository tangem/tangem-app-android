package com.tangem.tap.di

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.features.intentHandler.IntentProcessor
import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
import com.tangem.tap.features.intentHandler.handlers.OnPushClickedIntentHandler
import com.tangem.tap.features.intentHandler.handlers.WalletConnectLinkIntentHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object IntentHandlingModule {

    @Provides
    @Singleton
    fun provideBackgroundScanIntentHandler(): BackgroundScanIntentHandler = BackgroundScanIntentHandler()

    @Provides
    @Singleton
    fun provideWalletConnectLinkIntentHandler(): WalletConnectLinkIntentHandler = WalletConnectLinkIntentHandler()

    @Provides
    @Singleton
    fun provideOnPushClickedIntentHandler(analyticsEventHandler: AnalyticsEventHandler): OnPushClickedIntentHandler =
        OnPushClickedIntentHandler(analyticsEventHandler)

    @Provides
    @Singleton
    fun provideIntentProcessor(): IntentProcessor = IntentProcessor()
}