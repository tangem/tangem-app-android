package com.tangem.feature.swap.domain.models.domain

data class SavedSwapTransactionListModel(
    val userWalletId: String,
    val fromCryptoCurrencyId: String,
    val toCryptoCurrencyId: String,
    val transactions: List<SavedSwapTransactionModel>,
)

data class SavedSwapTransactionModel(
    val txId: String,
    val timestamp: Long,
    val fromCryptoAmount: String,
    val toCryptoAmount: String,
    val provider: SwapProvider,
    val toFiatAmount: String? = null,
    val fromFiatAmount: String? = null,
    val status: ExchangeStatusModel? = null,
)