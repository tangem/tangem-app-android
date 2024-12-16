package com.tangem.domain.tokens.legacy

import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action
import java.math.BigDecimal

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

    @Deprecated("Use AppRoute instead")
    data class SendToken(
        val userWallet: UserWallet,
        val tokenCurrency: CryptoCurrency.Token,
        val tokenFiatRate: BigDecimal?,
        val coinFiatRate: BigDecimal?,
        val feeCurrencyStatus: CryptoCurrencyStatus?,
        val transactionInfo: TransactionInfo? = null,
    ) : TradeCryptoAction()

    @Deprecated("Use AppRoute instead")
    data class SendCoin(
        val userWallet: UserWallet,
        val coinStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus?,
        val transactionInfo: TransactionInfo? = null,
    ) : TradeCryptoAction()

    data class TransactionInfo(
        val transactionId: String,
        val destinationAddress: String,
        val amount: String,
        val tag: String? = null,
    )
}