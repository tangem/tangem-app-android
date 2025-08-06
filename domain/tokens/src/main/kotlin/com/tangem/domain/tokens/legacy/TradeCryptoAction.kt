package com.tangem.domain.tokens.legacy

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import org.rekotlin.Action

sealed class TradeCryptoAction : Action {

    data class FinishSelling(val transactionId: String) : TradeCryptoAction()

    data class Sell(
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrencyCode: String,
    ) : TradeCryptoAction()
}