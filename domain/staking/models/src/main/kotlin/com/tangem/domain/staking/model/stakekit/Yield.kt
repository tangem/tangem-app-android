package com.tangem.domain.staking.model.stakekit

import com.tangem.domain.core.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
data class Yield(
    val id: String,
    val token: Token,
    val tokens: List<Token>,
    val args: Args,
    val status: Status,
    val apy: SerializedBigDecimal,
    val rewardRate: Double,
    val rewardType: RewardType,
    val metadata: Metadata,
    val validators: List<Validator>,
    val isAvailable: Boolean,
) {

    @Serializable
    data class Status(
        val enter: Boolean,
        val exit: Boolean?,
    )

    @Serializable
    data class Args(
        val enter: Enter,
        val exit: Enter?,
    ) {

        @Serializable
        data class Enter(
            val addresses: Addresses,
            val args: Map<String, AddressArgument>,
        ) {

            @Serializable
            data class Addresses(
                val address: AddressArgument,
                val additionalAddresses: Map<String, AddressArgument>? = null,
            )
        }
    }

    @Serializable
    data class Validator(
        val address: String,
        val status: String,
        val name: String,
        val image: String?,
        val website: String?,
        val apr: SerializedBigDecimal?,
        val commission: Double?,
        val stakedBalance: String?,
        val votingPower: Double?,
        val preferred: Boolean,
    )

    @Serializable
    data class Metadata(
        val name: String,
        val logoUri: String,
        val description: String,
        val documentation: String?,
        val gasFeeToken: Token,
        val token: Token,
        val tokens: List<Token>,
        val type: String,
        val rewardSchedule: String,
        val cooldownPeriod: Period,
        val warmupPeriod: Period,
        val rewardClaiming: String,
        val defaultValidator: String?,
        val minimumStake: Int?,
        val supportsMultipleValidators: Boolean,
        val revshare: Enabled,
        val fee: Enabled,
    ) {

        @Serializable
        data class Period(
            val days: Int,
        )

        @Serializable
        data class Enabled(
            val enabled: Boolean,
        )
    }

    enum class RewardType {
        APY, // compound rate
        APR, // simple rate
        UNKNOWN,
    }
}

@Serializable
data class Token(
    val name: String,
    val network: NetworkType,
    val symbol: String,
    val decimals: Int,
    val address: String?,
    val coinGeckoId: String?,
    val logoURI: String?,
    val isPoints: Boolean?,
)

@Serializable
data class AddressArgument(
    val required: Boolean,
    val network: String? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
)