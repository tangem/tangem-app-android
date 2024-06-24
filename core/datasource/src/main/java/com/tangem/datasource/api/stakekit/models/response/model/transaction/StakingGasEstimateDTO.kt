package com.tangem.datasource.api.stakekit.models.response.model.transaction

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class StakingGasEstimateDTO(
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "token")
    val token: TokenDTO,
    @Json(name = "gasLimit")
    val gasLimit: String?,
)
