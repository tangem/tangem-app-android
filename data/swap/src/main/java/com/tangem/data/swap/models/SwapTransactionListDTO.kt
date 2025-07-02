package com.tangem.data.swap.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.swap.models.SwapStatusModel
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
internal data class SwapTransactionListDTO(
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
    val transactions: List<SwapTransactionDTO>,
)

@JsonClass(generateAdapter = true)
internal data class SwapTransactionDTO(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "timestamp")
    val timestamp: Long,
    @Json(name = "fromCryptoAmount")
    val fromCryptoAmount: BigDecimal,
    @Json(name = "toCryptoAmount")
    val toCryptoAmount: BigDecimal,
    @Json(name = "provider")
    val provider: ExpressProvider,
    @Json(name = "status")
    val status: SwapStatusModel? = null,
)