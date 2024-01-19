package com.tangem.feature.swap.domain.models.domain

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

data class SavedSwapTransactionListModel(
    val userWalletId: String,
    val fromCryptoCurrencyId: String,
    val toCryptoCurrencyId: String,
    val fromCryptoCurrency: CryptoCurrency,
    val toCryptoCurrency: CryptoCurrency,
    val transactions: List<SavedSwapTransactionModel>,
)

/**
 * IMPORTANT !!!
 * This is internal model for storing swap tx. It should not be used outside.
 */
data class SavedSwapTransactionListModelInner(
    val userWalletId: String,
    val fromCryptoCurrencyId: String,
    val toCryptoCurrencyId: String,
    val fromTokensResponse: UserTokensResponse.Token? = null,
    val toTokensResponse: UserTokensResponse.Token? = null,
    val transactions: List<SavedSwapTransactionModel>,
)

data class SavedSwapTransactionModel(
    val txId: String,
    val timestamp: Long,
    val fromCryptoAmount: BigDecimal,
    val toCryptoAmount: BigDecimal,
    val provider: SwapProvider,
    val status: ExchangeStatusModel? = null,
)