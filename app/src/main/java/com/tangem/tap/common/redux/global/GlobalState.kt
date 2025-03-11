package com.tangem.tap.common.redux.global

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.features.onboarding.OnboardingManager
import com.tangem.tap.network.exchangeServices.CurrencyExchangeManager
import org.rekotlin.StateType

data class GlobalState(
    @Deprecated("Use scan response from selected user wallet")
    val scanResponse: ScanResponse? = null,
    val onboardingState: OnboardingState = OnboardingState(),
    val tapWalletManager: TapWalletManager = TapWalletManager(),
    val appCurrency: AppCurrency = AppCurrency.Default,
    val scanCardFailsCounter: Int = 0,
    val dialog: StateDialog? = null,
    val exchangeManager: CurrencyExchangeManager = CurrencyExchangeManager.dummy(),
    val isLastSignWithRing: Boolean = false,
) : StateType

typealias CryptoCurrencyName = String

data class OnboardingState(
    val onboardingStarted: Boolean = false,
    val onboardingManager: OnboardingManager? = null,
    val shouldResetOnCreate: Boolean = false,
)