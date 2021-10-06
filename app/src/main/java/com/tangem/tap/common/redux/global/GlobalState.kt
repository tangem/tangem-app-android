package com.tangem.tap.common.redux.global

import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.domain.PayIdManager
import com.tangem.tap.domain.TapWalletManager
import com.tangem.tap.domain.configurable.config.ConfigManager
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.features.feedback.FeedbackManager
import com.tangem.tap.features.onboarding.service.ProductOnboardingService
import com.tangem.tap.network.coinmarketcap.CoinMarketCapService
import com.tangem.tap.network.moonpay.MoonPayUserStatus
import org.rekotlin.StateType

data class GlobalState(
        val scanNoteResponse: ScanNoteResponse? = null,
        val cardVerifiedOnline: Boolean = false,
        val tapWalletManager: TapWalletManager = TapWalletManager(),
        val payIdManager: PayIdManager = PayIdManager(),
        val coinMarketCapService: CoinMarketCapService = CoinMarketCapService(),
        val configManager: ConfigManager? = null,
        val warningManager: WarningMessagesManager? = null,
        val feedbackManager: FeedbackManager? = null,
        val appCurrency: FiatCurrencyName = DEFAULT_FIAT_CURRENCY,
        val scanCardFailsCounter: Int = 0,
        val dialog: StateDialog? = null,
        val moonPayUserStatus: MoonPayUserStatus? = null,
        val onboardingService: ProductOnboardingService? = null,
) : StateType

typealias CryptoCurrencyName = String
typealias FiatCurrencyName = String