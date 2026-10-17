package com.tangem.data.networks.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import dev.onenowy.moshipolymorphicadapter.PolymorphicAdapterType
import dev.onenowy.moshipolymorphicadapter.annotations.NameLabel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Network status for storage in the local cache. Supports two types - the [Verified] and [NoAccount].
 *
 * @see [com.tangem.domain.models.network.NetworkStatus]
 */
@Serializable(with = NetworkStatusDMSerializer::class)
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
     * @property networkId           network id
     * @property derivationPath      derivation path
     * @property selectedAddress     selected address
     * @property availableAddresses  available addresses
     * @property amounts             amounts
     * @property yieldSupplyStatuses yield supply statuses
     */
    @NameLabel("amounts")
    @Serializable
    @JsonClass(generateAdapter = true)
    data class Verified(
        @SerialName("network_id")
        @Json(name = "network_id")
        override val networkId: ID,
        @SerialName("derivation_path")
        @Json(name = "derivation_path")
        override val derivationPath: DerivationPath,
        @SerialName("selected_address")
        @Json(name = "selected_address")
        override val selectedAddress: String,
        @SerialName("available_addresses")
        @Json(name = "available_addresses")
        override val availableAddresses: Set<Address>,
        @Json(name = "amounts") val amounts: List<CurrencyAmount>,
        @SerialName("yield_supply_statuses")
        @Json(name = "yield_supply_statuses")
        val yieldSupplyStatuses: List<YieldSupplyStatus>,
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
    @Serializable
    @JsonClass(generateAdapter = true)
    data class NoAccount(
        @SerialName("network_id")
        @Json(name = "network_id")
        override val networkId: ID,
        @SerialName("derivation_path")
        @Json(name = "derivation_path")
        override val derivationPath: DerivationPath,
        @SerialName("selected_address")
        @Json(name = "selected_address")
        override val selectedAddress: String,
        @SerialName("available_addresses")
        @Json(name = "available_addresses")
        override val availableAddresses: Set<Address>,
        @SerialName("amount_to_create_account")
        @Json(name = "amount_to_create_account")
        val amountToCreateAccount: SerializedBigDecimal,
        @SerialName("error_message")
        @Json(name = "error_message")
        val errorMessage: String,
    ) : NetworkStatusDM

    /**
     * Id
     *
     * @property value blockchain id [com.tangem.blockchain.common.Blockchain.id]
     */
    @Serializable
    @JsonClass(generateAdapter = true)
    data class ID(
        @Json(name = "value") val value: String,
    )

    @Serializable
    @JsonClass(generateAdapter = true)
    data class DerivationPath(
        @Json(name = "value") val value: String,
        @Json(name = "type") val type: Type,
    ) {

        @Serializable
        @JsonClass(generateAdapter = false)
        enum class Type {

            @SerialName("card")
            @Json(name = "card")
            CARD,

            @SerialName("custom")
            @Json(name = "custom")
            CUSTOM,

            @SerialName("none")
            @Json(name = "none")
            NONE,

            @SerialName("joint")
            @Json(name = "joint")
            JOINT,
        }
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class Address(
        @Json(name = "value") val value: String,
        @Json(name = "type") val type: Type,
    ) {

        @Serializable
        @JsonClass(generateAdapter = false)
        enum class Type {

            @SerialName("primary")
            @Json(name = "primary")
            Primary,

            @SerialName("secondary")
            @Json(name = "secondary")
            Secondary,
        }
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class CurrencyAmount(
        @Json(name = "id") val id: CurrencyId,
        @Json(name = "amount") val amount: SerializedBigDecimal,
    )

    @Serializable
    @JsonClass(generateAdapter = true)
    data class YieldSupplyStatus(
        @Json(name = "id") val id: CurrencyId,
        @SerialName("is_active")
        @Json(name = "is_active")
        val isActive: Boolean,
        @SerialName("is_initialized")
        @Json(name = "is_initialized")
        val isInitialized: Boolean,
        @SerialName("is_allowed_to_spend")
        @Json(name = "is_allowed_to_spend")
        val isAllowedToSpend: Boolean,
        @SerialName("effective_protocol_balance")
        @Json(name = "effective_protocol_balance")
        val effectiveProtocolBalance: SerializedBigDecimal? = null,
    )

    @Serializable
    @JsonClass(generateAdapter = true)
    data class CurrencyId(
        @Json(name = "value") val value: String,
    ) {

        companion object Companion {

            const val CONTRACT_ADDRESS_DELIMITER = '\u2693' // ⚓

            fun createCoinId(coinId: String): CurrencyId {
                return CurrencyId(value = coinId)
            }

            fun createTokenId(rawTokenId: String?, contractAddress: String): CurrencyId {
                return CurrencyId(
                    value = buildString {
                        if (rawTokenId != null) {
                            append(rawTokenId)
                        }
                        append(CONTRACT_ADDRESS_DELIMITER)
                        append(contractAddress)
                    },
                )
            }
        }
    }
}