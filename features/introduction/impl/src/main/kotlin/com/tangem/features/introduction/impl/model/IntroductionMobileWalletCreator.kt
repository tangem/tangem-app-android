package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

@ModelScoped
internal class IntroductionMobileWalletCreator @Inject constructor(
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
) {

    suspend fun createAndOpen(source: AnalyticsParam.ScreensSources, onFailed: () -> Unit) {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Started(source = source))
        createHotWalletUseCase(auth = HotAuth.NoAuth, mnemonicType = MnemonicType.Words12).fold(
            ifLeft = { error ->
                TangemLogger.e("Failed to create mobile wallet from Introduction", error)
                messageSender.send(
                    DialogMessage(
                        title = resourceReference(CoreResR.string.common_something_went_wrong),
                        message = resourceReference(CoreResR.string.common_unknown_error),
                    ),
                )
                onFailed()
            },
            ifRight = {
                val referralId = runSuspendCatching { appsFlyerRepository.getConversionData()?.refcode }.getOrNull()
                analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Finished(source = source))
                analyticsEventHandler.send(
                    OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                        source = source,
                        creationType = AnalyticsParam.WalletCreationType.NewSeed,
                        seedPhraseLength = SEED_PHRASE_LENGTH,
                        passPhraseState = AnalyticsParam.EmptyFull.Empty,
                        referralId = referralId,
                    ),
                )
                router.replaceAll(AppRoute.Wallet)
            },
        )
    }

    private companion object {
        const val SEED_PHRASE_LENGTH = 12
    }
}