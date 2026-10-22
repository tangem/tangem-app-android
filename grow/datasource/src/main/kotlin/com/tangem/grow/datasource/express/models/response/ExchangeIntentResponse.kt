package com.tangem.grow.datasource.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeIntentResponse(

    @Json(name = "intent")
    val intent: ExchangeIntent,

    @Json(name = "transactions")
    val transactions: List<ExchangeIntentTransaction>,
)

@JsonClass(generateAdapter = true)
data class ExchangeIntent(

    @Json(name = "intentId")
    val intentId: String,

    @Json(name = "clientId")
    val clientId: String?,

    @Json(name = "sessionId")
    val sessionId: String?,

    @Json(name = "walletId")
    val walletId: String,

    @Json(name = "provider")
    val provider: String,

    @Json(name = "rateType")
    val rateType: RateType,

    @Json(name = "fromNetwork")
    val fromNetwork: String,

    @Json(name = "fromContractAddress")
    val fromContractAddress: String,

    @Json(name = "fromDecimals")
    val fromDecimals: Int,

    @Json(name = "fromAmount")
    val fromAmount: String?,

    @Json(name = "fromAddress")
    val fromAddress: String?,

    @Json(name = "toNetwork")
    val toNetwork: String,

    @Json(name = "toContractAddress")
    val toContractAddress: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "toAmount")
    val toAmount: String?,

    @Json(name = "payoutAddress")
    val payoutAddress: String?,

    @Json(name = "toExtraId")
    val toExtraId: String?,
)

/** The exchange transactions carry more fields; the intent only needs to know that one was sent. */
@JsonClass(generateAdapter = true)
data class ExchangeIntentTransaction(

    @Json(name = "txId")
    val txId: String,
)