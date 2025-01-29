package com.tangem.feature.swap.domain.models.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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
@JsonClass(generateAdapter = true)
data class SavedSwapTransactionListModelInner(
    @Json(name = "userWalletId")
    val userWalletId: String,
    @Json(name = "fromCryptoCurrencyId")
    val fromCryptoCurrencyId: String,
    @Json(name = "toCryptoCurrencyId")
    val toCryptoCurrencyId: String,
    @Json(name = "fromTokensResponse")
    val fromTokensResponse: UserTokensResponse.Token? = null,
    @Json(name = "toTokensResponse")
    val toTokensResponse: UserTokensResponse.Token? = null,
    @Json(name = "transactions")
    val transactions: List<SavedSwapTransactionModel>,
)
// [REDACTED_TODO_COMMENT]
@JsonClass(generateAdapter = true)
data class SavedSwapTransactionModel(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "fromCryptoAmount")
    val fromCryptoAmount: BigDecimal,
    @Json(name = "toCryptoAmount")
    val toCryptoAmount: BigDecimal,
    @Json(name = "provider")
    val provider: SwapProvider,
    @Json(name = "status")
    val status: ExchangeStatusModel? = null,
)
