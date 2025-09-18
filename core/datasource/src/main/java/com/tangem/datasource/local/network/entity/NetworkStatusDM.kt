package com.tangem.datasource.local.network.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dev.onenowy.moshipolymorphicadapter.PolymorphicAdapterType
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import java.math.BigDecimal

/**
 * Network status for storage in the local cache. Supports two types - the [Verified] and [NoAccount].
 *
 * @see [com.tangem.domain.tokens.model.NetworkStatus]
 */
@JsonClass(generateAdapter = true, generator = PolymorphicAdapterType.NAME_POLYMORPHIC_ADAPTER)
sealed interface NetworkStatusDM {

    /** Network id */
    val networkId: ID

    /** Derivation path */
    val derivationPath: DerivationPath

    /** Selected address */
    val selectedAddress: String

    /** Available address */
    val availableAddresses: Set<Address>

    /**
     * Verified
     *
     * @property networkId          network id
     * @property derivationPath     derivation path
     * @property selectedAddress    selected address
     * @property availableAddresses available addresses
     * @property amounts            amounts
     */
    @NameLabel("amounts")
    data class Verified(
        @Json(name = "network_id") override val networkId: ID,
        @Json(name = "derivation_path") override val derivationPath: DerivationPath,
        @Json(name = "selected_address") override val selectedAddress: String,
        @Json(name = "available_addresses") override val availableAddresses: Set<Address>,
        @Json(name = "amounts") val amounts: Map<String, BigDecimal>,
        @Json(name = "yield_supply_statuses") val yieldSupplyStatuses: Map<String, YieldSupplyStatus?> = emptyMap(),
    ) : NetworkStatusDM

    /**
     * No account
     *
     * @property networkId             network id
     * @property derivationPath        derivation path
     * @property selectedAddress       selected address
     * @property availableAddresses    available addresses
     * @property amountToCreateAccount amount to create account
     * @property errorMessage          error message
     */
    @NameLabel("amount_to_create_account")
    data class NoAccount(
        @Json(name = "network_id") override val networkId: ID,
        @Json(name = "derivation_path") override val derivationPath: DerivationPath,
        @Json(name = "selected_address") override val selectedAddress: String,
        @Json(name = "available_addresses") override val availableAddresses: Set<Address>,
        @Json(name = "amount_to_create_account") val amountToCreateAccount: BigDecimal,
        @Json(name = "error_message") val errorMessage: String,
    ) : NetworkStatusDM

    @JsonClass(generateAdapter = true)
    data class ID(
        @Json(name = "value") val value: String,
    )

    @JsonClass(generateAdapter = true)
    data class DerivationPath(
        @Json(name = "value") val value: String,
        @Json(name = "type") val type: Type,
    ) {

        @JsonClass(generateAdapter = false)
        enum class Type {

            @Json(name = "card")
            CARD,

            @Json(name = "custom")
            CUSTOM,

            @Json(name = "none")
            NONE,
        }
    }

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

    @JsonClass(generateAdapter = true)
    data class YieldSupplyStatus(
        @Json(name = "is_active") val isActive: Boolean,
        @Json(name = "is_initialized") val isInitialized: Boolean,
        @Json(name = "is_allowed_to_spend") val isAllowedToSpend: Boolean,
    )
}