package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import java.math.BigDecimal

data class GasEstimateDTO(
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "token")
    val tokenDTO: TokenDTO,
    @Json(name = "gasLimit")
    val gasLimit: BigDecimal,
)
