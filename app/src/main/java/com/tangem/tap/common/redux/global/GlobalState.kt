package com.tangem.tap.common.redux.global

import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.feedback.LegacyFeedbackManager
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.onboarding.OnboardingManager
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import org.rekotlin.StateType

data class GlobalState(
    @Deprecated("Use scan response from selected user wallet")
    val scanResponse: ScanResponse? = null,
    val onboardingState: OnboardingState = OnboardingState(),
    val cardVerifiedOnline: Boolean = false,
    val tapWalletManager: TapWalletManager = TapWalletManager(),
    val configManager: ConfigManager? = null,
    val warningManager: WarningMessagesManager? = null,
    val feedbackManager: LegacyFeedbackManager? = null,
    val appCurrency: AppCurrency = AppCurrency.Default,
    val scanCardFailsCounter: Int = 0,
    val dialog: StateDialog? = null,
    val exchangeManager: CurrencyExchangeManager = CurrencyExchangeManager.dummy(),
    val userCountryCode: String? = null,
    val appThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
) : StateType

typealias CryptoCurrencyName = String

data class OnboardingState(
    val onboardingStarted: Boolean = false,
    val onboardingManager: OnboardingManager? = null,
    val shouldResetOnCreate: Boolean = false,
)