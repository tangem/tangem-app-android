package com.tangem.domain.express.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Express provider type
 *
 * @property typeName provider type name
 */
@JsonClass(generateAdapter = false)
enum class ExpressProviderType(val typeName: String) {
    @Json(name = "DEX")
    DEX(typeName = "DEX"),

    @Json(name = "CEX")
    CEX(typeName = "CEX"),

    @Json(name = "DEX_BRIDGE")
    DEX_BRIDGE(typeName = "DEX/Bridge"),

    @Json(name = "onramp")
    ONRAMP(typeName = "ONRAMP"),
    ;

    fun shouldStoreSwapTransaction() = when (this) {
        CEX,
        DEX_BRIDGE,
        DEX,
        -> true
        ONRAMP,
        -> false
    }

    companion object {
        fun getSwapProviderTypes(): List<ExpressProviderType> {
            return listOf(CEX, DEX, DEX_BRIDGE)
        }
    }
}