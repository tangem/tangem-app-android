package com.tangem.datasource.local.network.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.tokens.model.Network
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import java.math.BigDecimal

internal sealed interface NetworkStatusDM {

    val networkId: Network.ID
    val selectedAddress: String
    val availableAddresses: Set<Address>

    @NameLabel("amounts")
    data class Verified(
        @Json(name = "network_id") override val networkId: Network.ID,
        @Json(name = "selected_address") override val selectedAddress: String,
        @Json(name = "available_addresses") override val availableAddresses: Set<Address>,
        @Json(name = "amounts") val amounts: Map<String, BigDecimal>,
    ) : NetworkStatusDM

    @NameLabel("amount_to_create_account")
    data class NoAccount(
        @Json(name = "network_id") override val networkId: Network.ID,
        @Json(name = "selected_address") override val selectedAddress: String,
        @Json(name = "available_addresses") override val availableAddresses: Set<Address>,
        @Json(name = "amount_to_create_account") val amountToCreateAccount: BigDecimal,
        @Json(name = "error_message") val errorMessage: String,
    ) : NetworkStatusDM

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