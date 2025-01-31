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

    val preferredValidators: List<Validator>
        get() = validators.filter { it.preferred }

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
            val args: Map<ArgType, AddressArgument>,
        ) {

            @Serializable
            data class Addresses(
                val address: AddressArgument,
                val additionalAddresses: Map<ArgType, AddressArgument>? = null,
            )
        }

        enum class ArgType {
            ADDRESS,
            AMOUNT,
            UNKNOWN,
        }
    }

    @Serializable
    data class Validator(
        val address: String,
        val status: ValidatorStatus,
        val name: String,
        val image: String? = null,
        val website: String? = null,
        val apr: SerializedBigDecimal? = null,
        val commission: Double? = null,
        val stakedBalance: String? = null,
        val votingPower: Double? = null,
        val preferred: Boolean,
        val isStrategicPartner: Boolean,
    ) {

        enum class ValidatorStatus {
            ACTIVE,
            DEACTIVATING,
            INACTIVE,
            JAILED,
            FULL,
            UNKNOWN,
        }
    }

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
        val rewardSchedule: RewardSchedule,
        val cooldownPeriod: Period?,
        val warmupPeriod: Period,
        val rewardClaiming: RewardClaiming,
        val defaultValidator: String?,
        val minimumStake: Int?,
        val supportsMultipleValidators: Boolean?,
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

        enum class RewardSchedule {
            BLOCK,
            WEEK,
            HOUR,
            DAY,
            MONTH,
            ERA,
            EPOCH,

            UNKNOWN,
        }

        enum class RewardClaiming {
            AUTO,
            MANUAL,

            UNKNOWN,
        }
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
    val minimum: SerializedBigDecimal? = null,
    val maximum: SerializedBigDecimal? = null,
)