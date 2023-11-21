package com.tangem.tap.features.intentHandler.handlers

import android.content.Intent
import com.tangem.tap.features.intentHandler.IntentHandler

/**
 * @author Anton Zhilenkov on 04.06.2023.
 */
class BuyCurrencyIntentHandler : IntentHandler {

    override fun handleIntent(intent: Intent?): Boolean {
        // FIXME: https://tangem.atlassian.net/browse/AND-5311
        // val data = intent?.data ?: return false
        // val currency = store.state.walletState.selectedCurrency ?: return false
        //
        // val successUri = Uri.parse(ExchangeUrlBuilder.SUCCESS_URL)
        // return if (data.host == successUri.host && data.authority == successUri.authority) {
        //     val currencyType = AnalyticsParam.CurrencyType.Currency(currency)
        //     Analytics.send(TokenScreenAnalyticsEvent.Bought(currencyType.value))
        //     true
        // } else {
        //     false
        // }

        return false
    }
}
