package com.tangem.tap.di

import com.tangem.core.navigation.feedback.FeedbackManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.tap.common.feedback.ProxyFeedbackManager
import com.tangem.tap.common.url.CustomTabsUrlOpener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object UtilsModule {

    @Provides
    @Singleton
    fun provideUrlOpener(): UrlOpener = CustomTabsUrlOpener

    @Provides
    @Singleton
    fun provideFeedbackManager(): FeedbackManager = ProxyFeedbackManager()
}