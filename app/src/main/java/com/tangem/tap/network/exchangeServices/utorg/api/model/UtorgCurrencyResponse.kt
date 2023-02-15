package com.tangem.tap.network.exchangeServices.utorg.api.model

import com.squareup.moshi.Json
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgErrorResponse
import com.tangem.tap.network.exchangeServices.utorg.api.UtorgResponse

class UtorgCurrencyResponse(
    @Json(name = "success") override val success: Boolean,
    @Json(name = "timestamp") override val timestamp: Long,
    @Json(name = "data") override val data: List<UtorgCurrencyData>?,
    @Json(name = "error") override val error: UtorgErrorResponse?,
) : UtorgResponse<List<UtorgCurrencyData>>

data class UtorgCurrencyData(
    @Json(name = "currency") val currency: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "type") val type: UtorgCurrencyType,
    @Json(name = "caption") val caption: String?,
    @Json(name = "chain") val chain: String?,
)

enum class UtorgCurrencyType {
    FIAT, CRYPTO
}