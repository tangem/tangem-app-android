package com.tangem.tap.di

import com.tangem.core.navigation.review.ReviewManager
import com.tangem.tap.GoogleReviewManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class GoogleReviewManagerModule {

    @Singleton
    @Provides
    fun provideGoogleReviewManager(): ReviewManager {
        return GoogleReviewManager()
    }
}