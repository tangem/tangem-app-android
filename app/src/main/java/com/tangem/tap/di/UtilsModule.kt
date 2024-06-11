package com.tangem.tap.di

import com.tangem.core.navigation.feedback.FeedbackManager
import com.tangem.tap.common.feedback.ProxyFeedbackManager
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
    fun provideFeedbackManager(): FeedbackManager = ProxyFeedbackManager()
}