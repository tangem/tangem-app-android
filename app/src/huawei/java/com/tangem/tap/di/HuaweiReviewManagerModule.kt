package com.tangem.tap.di

import com.tangem.core.navigation.review.DummyReviewManager
import com.tangem.core.navigation.review.ReviewManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class HuaweiReviewManagerModule {

    @Provides
    @Singleton
    fun provideHuaweiReviewManager(): ReviewManager {
        return DummyReviewManager()
    }
}