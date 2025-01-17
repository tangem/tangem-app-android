package com.tangem.tap.di

import android.content.Context
import com.tangem.domain.card.BuildConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.sdk.impl.DefaultTangemSdkManager
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import com.tangem.tap.domain.tasks.visa.VisaCardActivationTask
import com.tangem.tap.domain.visa.VisaCardScanHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class TangemSdkManagerModule {

    @Provides
    @Singleton
    fun provideTangemSdkManager(
        @ApplicationContext context: Context,
        cardSdkConfigRepository: CardSdkConfigRepository,
        visaCardScanHandler: VisaCardScanHandler,
        visaCardActivationTaskFactory: VisaCardActivationTask.Factory,
        onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    ): TangemSdkManager {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockTangemSdkManager(resources = context.resources)
        } else {
            DefaultTangemSdkManager(
                cardSdkConfigRepository = cardSdkConfigRepository,
                resources = context.resources,
                visaCardScanHandler = visaCardScanHandler,
                visaCardActivationTaskFactory = visaCardActivationTaskFactory,
                onboardingV2FeatureToggles = onboardingV2FeatureToggles,
            )
        }
    }
}