package com.tangem.datasource.api.gasless.models.tron

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TronEstimateRequestBody(
    @Json(name = "fromAddress") val fromAddress: String,
    @Json(name = "toAddress") val toAddress: String,
    @Json(name = "tokenContract") val tokenContract: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "feeTokenContract") val feeTokenContract: String?,
)

@JsonClass(generateAdapter = true)
data class TronEstimateResponse(
    @Json(name = "quoteId") val quoteId: String,
    @Json(name = "feeRecipient") val feeRecipient: String,
    @Json(name = "compensationToken") val compensationToken: String,
    @Json(name = "compensationAmount") val compensationAmount: String,
    @Json(name = "compensationAmountRaw") val compensationAmountRaw: String,
    @Json(name = "estimate") val estimate: TronEstimateBreakdown,
    @Json(name = "expiresAt") val expiresAt: String,
)

@JsonClass(generateAdapter = true)
data class TronEstimateBreakdown(
    @Json(name = "energy") val energy: Long,
    @Json(name = "bandwidth") val bandwidth: Long,
    @Json(name = "trxCost") val trxCost: String,
)

@JsonClass(generateAdapter = true)
data class TronSubmitRequestBody(
    @Json(name = "quoteId") val quoteId: String,
    @Json(name = "signedCompensationTx") val signedCompensationTx: String,
    @Json(name = "signedOriginalTx") val signedOriginalTx: String,
)

@JsonClass(generateAdapter = true)
data class TronSubmitResponse(
    @Json(name = "compensationTxHash") val compensationTxHash: String,
    @Json(name = "originalTxHash") val originalTxHash: String,
    @Json(name = "status") val status: String,
)

@JsonClass(generateAdapter = true)
data class TronTokensResponse(
    @Json(name = "tokens") val tokens: List<TronTokenDto>,
)

@JsonClass(generateAdapter = true)
data class TronTokenDto(
    @Json(name = "address") val address: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "chain") val chain: String,
)