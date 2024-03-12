package com.tangem.tap.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.domain.feedback.FeedbackManagerFeatureToggles
import com.tangem.tap.common.feedback.DefaultFeedbackManagerFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedbackManagerFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideFeedbackManagerFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): FeedbackManagerFeatureToggles {
        return DefaultFeedbackManagerFeatureToggles(featureTogglesManager)
    }
}