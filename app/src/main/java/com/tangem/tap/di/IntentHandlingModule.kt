package com.tangem.tap.di

import com.tangem.tap.features.intentHandler.handlers.BackgroundScanIntentHandler
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
}