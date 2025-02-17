package com.tangem.datasource.local.network.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.Network
import java.math.BigDecimal

internal typealias NetworkStatusesByWalletId = Map<String, Set<NetworkStatusDM>>

@JsonClass(generateAdapter = true)
internal data class NetworkStatusDM(
    @Json(name = "network_id") val networkId: Network.ID,
    @Json(name = "selected_address") val selectedAddress: String,
    @Json(name = "available_addresses") val availableAddresses: Set<Address>,
    @Json(name = "amounts") val amounts: Map<String, BigDecimal>,
) {

    @JsonClass(generateAdapter = true)
    data class Address(
        @Json(name = "value") val value: String,
        @Json(name = "type") val type: Type,
    ) {

        @JsonClass(generateAdapter = false)
        enum class Type {

            @Json(name = "primary")
            Primary,

            @Json(name = "secondary")
            Secondary,
        }
    }
}