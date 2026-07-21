package com.tangem.data.staking.verification.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class EvmStakingTx(
    @Json(name = "from") val from: String? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "value") val value: String? = null,
)