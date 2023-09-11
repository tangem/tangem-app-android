package com.tangem.domain.tokens.legacy

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action
import java.math.BigDecimal

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

        data class SendToken(
            val userWallet: UserWallet,
            val tokenStatus: CryptoCurrencyStatus,
            val coinFiatRate: BigDecimal?,
        ) : New()

        data class SendCoin(val userWallet: UserWallet, val coinStatus: CryptoCurrencyStatus) : New()

        data class Swap(val cryptoCurrency: CryptoCurrency) : New()
    }
}