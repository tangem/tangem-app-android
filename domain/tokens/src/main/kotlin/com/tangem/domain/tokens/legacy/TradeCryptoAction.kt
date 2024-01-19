package com.tangem.domain.tokens.legacy

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action
import java.math.BigDecimal

sealed class TradeCryptoAction : Action {

    data class SendCrypto(
        val currencyId: String,
        val amount: String,
        val destinationAddress: String,
        val transactionId: String,
    ) : TradeCryptoAction()

    data class FinishSelling(val transactionId: String) : TradeCryptoAction()

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

        data class SendToken(
            val userWallet: UserWallet,
            val tokenCurrency: CryptoCurrency.Token,
            val tokenFiatRate: BigDecimal?,
            val coinFiatRate: BigDecimal?,
            val feeCurrencyStatus: CryptoCurrencyStatus?,
        ) : New()

        data class SendCoin(
            val userWallet: UserWallet,
            val coinStatus: CryptoCurrencyStatus,
            val feeCurrencyStatus: CryptoCurrencyStatus?,
        ) : New()

        data class Swap(val cryptoCurrency: CryptoCurrency) : New()
    }
}