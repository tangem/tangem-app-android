package com.tangem.domain.tokens.legacy

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action

sealed class TradeCryptoAction : Action {

    data class Buy(val checkUserLocation: Boolean = true) : TradeCryptoAction()

    object Sell : TradeCryptoAction()

    data class SendCrypto(
        val currencyId: String,
        val amount: String,
        val destinationAddress: String,
        val transactionId: String,
    ) : TradeCryptoAction()

    data class FinishSelling(val transactionId: String) : TradeCryptoAction()

    object Swap : TradeCryptoAction()

    sealed class New : TradeCryptoAction() {

        data class Buy(
            val userWallet: UserWallet,
            val cryptoCurrencyStatus: CryptoCurrencyStatus,
            val appCurrencyCode: String,
            val checkUserLocation: Boolean = true,
        ) : New()

        data class Sell(
            val cryptoCurrencyStatus: CryptoCurrencyStatus,
            val appCurrencyCode: String,
        ) : New()

        object Send : New()

        data class Swap(val cryptoCurrency: CryptoCurrency) : New()
    }
}