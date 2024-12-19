package com.tangem.domain.tokens.legacy

import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action

sealed class TradeCryptoAction : Action {

    data class FinishSelling(val transactionId: String) : TradeCryptoAction()

    data class Buy(
        val userWallet: UserWallet,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val source: OnrampSource,
        val appCurrencyCode: String,
        val checkUserLocation: Boolean = true,
    ) : TradeCryptoAction()

    data class Sell(
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrencyCode: String,
    ) : TradeCryptoAction()
}