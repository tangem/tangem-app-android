package com.tangem.tap.common.redux.global

import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.analytics.AnalyticsHandler
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.feedback.FeedbackManager
import com.tangem.tap.features.onboarding.OnboardingManager
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import org.rekotlin.StateType

data class GlobalState(
    val scanResponse: ScanResponse? = null,
    val onboardingState: OnboardingState = OnboardingState(),
    val cardVerifiedOnline: Boolean = false,
    val tapWalletManager: TapWalletManager = TapWalletManager(),
    val payIdManager: PayIdManager = PayIdManager(),
    val configManager: ConfigManager? = null,
    val warningManager: WarningMessagesManager? = null,
    val feedbackManager: FeedbackManager? = null,
    val appCurrency: FiatCurrencyName = DEFAULT_FIAT_CURRENCY,
    val scanCardFailsCounter: Int = 0,
    val dialog: StateDialog? = null,
    val currencyExchangeManager: CurrencyExchangeManager? = null,
    val resources: AndroidResources = AndroidResources(),
    val analyticsHandlers: AnalyticsHandler? = null,
) : StateType


data class AndroidResources(
    val strings: RString = RString(),
) {
    data class RString(
        val addressWasCopied: Int = -1,
        val walletIsNotEmpty: Int = -1,
    )
}
typealias CryptoCurrencyName = String
typealias FiatCurrencyName = String

data class OnboardingState(
    val onboardingStarted: Boolean = false,
    val onboardingManager: OnboardingManager? = null,
)