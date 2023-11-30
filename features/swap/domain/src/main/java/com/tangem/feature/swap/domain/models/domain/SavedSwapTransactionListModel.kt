package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

data class SavedSwapTransactionListModel(
    val userWalletId: String,
    val fromCryptoCurrencyId: String,
    val toCryptoCurrencyId: String,
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