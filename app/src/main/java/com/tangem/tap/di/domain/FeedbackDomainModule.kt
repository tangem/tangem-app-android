package com.tangem.tap.di.domain

import android.content.Context
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.GetFeedbackEmailUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
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
    fun provideGetCardInfoUseCase(feedbackRepository: FeedbackRepository): GetCardInfoUseCase {
        return GetCardInfoUseCase(feedbackRepository = feedbackRepository)
    }

    @Provides
    @Singleton
    fun provideGetFeedbackEmailUseCase(
        feedbackRepository: FeedbackRepository,
        @ApplicationContext context: Context,
    ): GetFeedbackEmailUseCase {
        return GetFeedbackEmailUseCase(feedbackRepository = feedbackRepository, resources = context.resources)
    }

    @Provides
    @Singleton
    fun provideSaveBlockchainErrorUseCase(feedbackRepository: FeedbackRepository): SaveBlockchainErrorUseCase {
        return SaveBlockchainErrorUseCase(feedbackRepository = feedbackRepository)
    }
}