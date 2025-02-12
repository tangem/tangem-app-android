package com.tangem.datasource.local.network.entity

import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.Network
import java.math.BigDecimal

internal typealias NetworkStatusesDM = Map<String, Set<NetworkStatusDM>>

@JsonClass(generateAdapter = true)
internal data class NetworkStatusDM(
    val networkId: Network.ID,
    val selectedAddress: String,
    val availableAddresses: Set<Address>,
    val amounts: Map<String, BigDecimal>,
) {

    @JsonClass(generateAdapter = true)
    data class Address(
        val value: String,
        val type: Type,
    ) {

        enum class Type {
            Primary, Secondary,
        }
    }
}