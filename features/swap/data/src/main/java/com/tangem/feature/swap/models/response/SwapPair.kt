package com.tangem.feature.swap.models.response

import com.squareup.moshi.Json

data class SwapPair(
    @Json(name = "from")
    val from: String,

    @Json(name = "to")
    val to: String,

    @Json(name = "providers")
    val providers: List<SwapPairProvider>,

)

data class SwapPairProvider(
    @Json(name = "providerId")
    val providerId: Int,

    @Json(name = "providerId")
    val rateType: RateType,
)

enum class RateType {
    @Json(name = "float")
    FLOAT,

    @Json(name = "fixed")
    FIXED,
}