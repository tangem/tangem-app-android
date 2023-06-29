package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import android.net.Uri
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Token
import com.tangem.tap.features.intentHandler.IntentHandler
import com.tangem.tap.network.exchangeServices.ExchangeUrlBuilder
import com.tangem.tap.store

/**
[REDACTED_AUTHOR]
 */
class BuyCurrencyIntentHandler : IntentHandler {

    override suspend fun handleIntent(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        val currency = store.state.walletState.selectedCurrency ?: return false

        val successUri = Uri.parse(ExchangeUrlBuilder.SUCCESS_URL)
        return if (data.host == successUri.host && data.authority == successUri.authority) {
            val currencyType = AnalyticsParam.CurrencyType.Currency(currency)
            Analytics.send(Token.Bought(currencyType))
            true
        } else {
            false
        }
    }
}