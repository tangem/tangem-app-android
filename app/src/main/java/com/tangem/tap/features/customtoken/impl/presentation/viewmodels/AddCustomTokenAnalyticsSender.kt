package com.tangem.tap.features.customtoken.impl.presentation.viewmodels

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.features.wallet.models.Currency

/** Analytics sender for tokens list screen */
class AddCustomTokenAnalyticsSender(private val analyticsEventHandler: AnalyticsEventHandler) {

    fun sendWhenScreenOpened() {
        analyticsEventHandler.send(ManageTokens.CustomToken.ScreenOpened)
    }

    fun sendWhenAddTokenButtonClicked(currency: Currency, address: String) {
        analyticsEventHandler.send(
            when (currency) {
                is Currency.Blockchain -> {
                    ManageTokens.CustomToken.TokenWasAdded.Blockchain(
                        derivationPath = currency.derivationPath,
                        blockchain = currency.blockchain,
                    )
                }

                is Currency.Token -> {
                    ManageTokens.CustomToken.TokenWasAdded.Token(
                        symbol = currency.currencySymbol,
                        derivationPath = currency.derivationPath,
                        blockchain = currency.blockchain,
                        contractAddress = address,
                    )
                }
            },
        )
    }
}