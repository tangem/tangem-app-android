package com.tangem.tap.di

import android.content.Context
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.domain.card.BuildConfig
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.wallets.derivations.DerivationsHelper
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.domain.sdk.impl.DefaultTangemSdkManager
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import com.tangem.tap.domain.tasks.tangempay.TangemPayGenerateAddressAndSignChallengeTask
import com.tangem.tap.domain.tasks.tangempay.TangemPayGenerateMoneyAccountAddressTask
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
        tangemPayChallengeTask: TangemPayGenerateAddressAndSignChallengeTask,
        tangemPayMoneyAccountTask: TangemPayGenerateMoneyAccountAddressTask,
        analyticsEventHandler: AnalyticsEventHandler,
        analyticsErrorHandler: AnalyticsErrorHandler,
        cardContextInterceptorFactory: CardContextInterceptor.Factory,
        cardRepository: CardRepository,
        derivationsHelper: DerivationsHelper,
        walletRegistrationLauncher: Lazy<WalletRegistrationLauncher>,
        deviceRegistrar: Lazy<DeviceRegistrar>,
        walletRegistrar: Lazy<WalletRegistrar>,
        foregroundActivityProvider: ForegroundActivityProvider,
    ): TangemSdkManager {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockTangemSdkManager(foregroundActivityProvider, deviceRegistrar, walletRegistrar)
        } else {
            DefaultTangemSdkManager(
                cardSdkConfigRepository = cardSdkConfigRepository,
                resources = context.resources,
                tangemPayChallengeTask = tangemPayChallengeTask,
                tangemPayMoneyAccountTask = tangemPayMoneyAccountTask,
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