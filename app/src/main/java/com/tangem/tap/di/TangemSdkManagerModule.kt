package com.tangem.tap.di

import android.content.Context
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.domain.card.BuildConfig
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.wallets.derivations.DerivationsHelper
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.domain.sdk.impl.DefaultTangemSdkManager
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import com.tangem.tap.domain.tasks.visa.TangemPayGenerateAddressAndSignChallengeTask
import com.tangem.tap.domain.tasks.visa.TangemPayGenerateMoneyAccountAddressTask
import com.tangem.tap.domain.tasks.visa.VisaCardActivationTask
import com.tangem.tap.domain.visa.VisaCardScanHandler
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import dagger.Lazy
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
        tangemPayChallengeTask: TangemPayGenerateAddressAndSignChallengeTask,
        tangemPayMoneyAccountTask: TangemPayGenerateMoneyAccountAddressTask,
        onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
        analyticsEventHandler: AnalyticsEventHandler,
        analyticsErrorHandler: AnalyticsErrorHandler,
        cardContextInterceptorFactory: CardContextInterceptor.Factory,
        cardRepository: CardRepository,
        derivationsHelper: DerivationsHelper,
        walletRegistrationLauncher: Lazy<WalletRegistrationLauncher>,
        foregroundActivityProvider: ForegroundActivityProvider,
    ): TangemSdkManager {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockTangemSdkManager(foregroundActivityProvider)
        } else {
            DefaultTangemSdkManager(
                cardSdkConfigRepository = cardSdkConfigRepository,
                resources = context.resources,
                visaCardScanHandler = visaCardScanHandler,
                visaCardActivationTaskFactory = visaCardActivationTaskFactory,
                tangemPayChallengeTask = tangemPayChallengeTask,
                tangemPayMoneyAccountTask = tangemPayMoneyAccountTask,
                onboardingV2FeatureToggles = onboardingV2FeatureToggles,
                analyticsEventHandler = analyticsEventHandler,
                analyticsErrorHandler = analyticsErrorHandler,
                cardContextInterceptorFactory = cardContextInterceptorFactory,
                cardRepository = cardRepository,
                derivationsHelper = derivationsHelper,
                walletRegistrationLauncher = walletRegistrationLauncher,
            )
        }
    }
}