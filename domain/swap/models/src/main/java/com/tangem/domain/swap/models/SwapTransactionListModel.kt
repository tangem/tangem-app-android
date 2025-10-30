package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigDecimal

/**
 * List of saved swap transactions
 */
data class SwapTransactionListModel(
    val userWalletId: String,
    val fromCryptoCurrencyId: String,
    val toCryptoCurrencyId: String,
    val fromCryptoCurrency: CryptoCurrency,
    val toCryptoCurrency: CryptoCurrency,
    val fromAccount: Account.CryptoPortfolio?,
    val toAccount: Account.CryptoPortfolio?,
    val transactions: List<SwapTransactionModel>,
)

/**
 * Saved swap transactions
 */
data class SwapTransactionModel(
    val txId: String,
    val timestamp: Long,
    val fromCryptoAmount: BigDecimal,
    val toCryptoAmount: BigDecimal,
    val provider: ExpressProvider,
    val status: SwapStatusModel? = null,
    val swapTxType: SwapTxType? = SwapTxType.Swap,
)