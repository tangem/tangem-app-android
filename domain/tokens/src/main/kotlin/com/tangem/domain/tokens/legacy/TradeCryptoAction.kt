package com.tangem.domain.tokens.legacy

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import org.rekotlin.Action
import java.math.BigDecimal

sealed class TradeCryptoAction : Action {

    data class FinishSelling(val transactionId: String) : TradeCryptoAction()

    data class Buy(
        val userWallet: UserWallet,
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrencyCode: String,
        val checkUserLocation: Boolean = true,
    ) : TradeCryptoAction()

    data class Sell(
        val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val appCurrencyCode: String,
    ) : TradeCryptoAction()

    data class SendToken(
        val userWallet: UserWallet,
        val tokenCurrency: CryptoCurrency.Token,
        val tokenFiatRate: BigDecimal?,
        val coinFiatRate: BigDecimal?,
        val feeCurrencyStatus: CryptoCurrencyStatus?,
        val transactionInfo: TransactionInfo? = null,
    ) : TradeCryptoAction()

    data class SendCoin(
        val userWallet: UserWallet,
        val coinStatus: CryptoCurrencyStatus,
        val feeCurrencyStatus: CryptoCurrencyStatus?,
        val transactionInfo: TransactionInfo? = null,
    ) : TradeCryptoAction()

    data class Swap(val cryptoCurrency: CryptoCurrency) : TradeCryptoAction()

    data class Stake(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
        val yield: Yield,
    ) : TradeCryptoAction()

    data class TransactionInfo(
        val transactionId: String,
        val destinationAddress: String,
        val amount: String,
        val tag: String? = null,
    )
}