package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

data class SwapResponse(
    @Json(name = "fromToken") val fromToken: TokenOneInchDto,
    @Json(name = "toToken") val toToken: TokenOneInchDto,
    @Json(name = "toAmount") val toTokenAmount: String,
    @Json(name = "tx") val transaction: TransactionDto,
)