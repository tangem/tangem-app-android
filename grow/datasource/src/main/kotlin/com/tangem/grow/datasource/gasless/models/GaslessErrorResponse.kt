package com.tangem.grow.datasource.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Body of a failed gasless-service call. */
@JsonClass(generateAdapter = true)
data class GaslessErrorResponse(
    @Json(name = "error") val error: GaslessErrorDto?,
)

@JsonClass(generateAdapter = true)
data class GaslessErrorDto(
    @Json(name = "code") val code: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "details") val details: GaslessErrorDetailsDto?,
)

/**
 * Amounts are read as strings so a JSON number keeps its exact base-unit value instead of passing
 * through Double. [signature] and [status] are populated by the backend on Solana UNCERTAIN / 409
 * responses.
 */
@JsonClass(generateAdapter = true)
data class GaslessErrorDetailsDto(
    @Json(name = "minimum") val minimum: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "signature") val signature: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "balance") val balance: String? = null,
    @Json(name = "required") val required: String? = null,
    @Json(name = "allowance") val allowance: String? = null,
    @Json(name = "allowances") val allowances: Map<String, String>? = null,
)