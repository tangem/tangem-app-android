package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class ConstructTransactionRequestBody(
    @Json(name = "gasArgs")
    val gasArgs: GasArgs? = null,
    @Json(name = "ledgerWalletAPICompatible")
    val ledgerWalletAPICompatible: Boolean? = null,
) {
    @JsonClass(generateAdapter = true)
    data class GasArgs(
        // cosmos-specific
        @Json(name = "gasPrice")
        val gasPrice: BigDecimal? = null,
        // EVM eip 1559 specific
        @Json(name = "type")
        val type: Int? = null,
        @Json(name = "maxFeePerGas")
        val maxFeePerGas: BigDecimal? = null,
        @Json(name = "maxPriorityFeePerGas")
        val maxPriorityFeePerGas: BigDecimal? = null,
    )
}