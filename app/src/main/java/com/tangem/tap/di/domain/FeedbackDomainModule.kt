package com.tangem.tap.di.domain

import android.content.Context
import com.tangem.domain.feedback.GetSupportFeedbackEmailUseCase
import com.tangem.domain.feedback.repository.FeedbackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeedbackDomainModule {

    @Provides
    @Singleton
    fun provideGetFeedbackToSupportUseCase(
        feedbackRepository: FeedbackRepository,
        @ApplicationContext context: Context,
    ): GetSupportFeedbackEmailUseCase {
        return GetSupportFeedbackEmailUseCase(feedbackRepository = feedbackRepository, resources = context.resources)
    }
}
