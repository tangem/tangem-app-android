package com.tangem.tap.di.data

import com.tangem.datasource.local.config.environment.EnvironmentConfig
import com.tangem.datasource.local.config.environment.models.SurveySparrowSwapRatingConfig
import com.tangem.store.datasource.config.StoreEnvironmentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StoreConfigModule {

    @Provides
    @Singleton
    fun provideStoreEnvironmentConfig(environmentConfig: EnvironmentConfig): StoreEnvironmentConfig {
        return StoreEnvironmentConfig(
            amplitudeApiKey = environmentConfig.amplitudeApiKey,
            amplitudeApiKeyDev = environmentConfig.amplitudeApiKeyDev,
            appsFlyerApiKey = environmentConfig.appsFlyerApiKey,
            appsAppId = environmentConfig.appsAppId,
            customerIoCdpApiKey = environmentConfig.customerIoCdpApiKey,
            surveySparrowToken = environmentConfig.surveySparrowToken,
            surveySparrowSwapRating = environmentConfig.surveySparrowSwapRating?.toStoreSwapRating(),
            blockAidApiKey = environmentConfig.blockAidApiKey,
            tangemApiKey = environmentConfig.tangemApiKey,
            tangemApiKeyDev = environmentConfig.tangemApiKeyDev,
            tangemApiKeyStage = environmentConfig.tangemApiKeyStage,
        )
    }

    private fun SurveySparrowSwapRatingConfig.toStoreSwapRating(): StoreEnvironmentConfig.SurveySparrowSwapRating {
        return StoreEnvironmentConfig.SurveySparrowSwapRating(
            surveyId = surveyId,
            ratingQuestionId = ratingQuestionId,
            feedbackQuestionId = feedbackQuestionId,
        )
    }
}