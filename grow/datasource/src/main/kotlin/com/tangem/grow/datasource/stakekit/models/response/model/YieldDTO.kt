@file:Suppress("BooleanPropertyNaming")

package com.tangem.grow.datasource.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class YieldDTO(
    @Json(name = "id")
    val id: String?,
    @Json(name = "token")
    val token: TokenDTO?,
    @Json(name = "tokens")
    val tokens: List<TokenDTO>?,
    @Json(name = "args")
    val args: ArgsDTO?,
    @Json(name = "status")
    val status: StatusDTO?,
    @Json(name = "apy")
    val apy: SerializedBigDecimal?,
    @Json(name = "rewardRate")
    val rewardRate: Double?,
    @Json(name = "rewardType")
    val rewardType: RewardTypeDTO?,
    @Json(name = "metadata")
    val metadata: MetadataDTO?,
    @Json(name = "validators")
    val validators: List<ValidatorDTO>?,
    @Json(name = "isAvailable")
    val isAvailable: Boolean?,
) {

    @Serializable
    @JsonClass(generateAdapter = true)
    data class StatusDTO(
        @Json(name = "enter")
        val enter: Boolean?,
        @Json(name = "exit")
        val exit: Boolean?,
    )

    @Serializable
    @JsonClass(generateAdapter = true)
    data class ArgsDTO(
        @Json(name = "enter")
        val enter: Enter?,
        @Json(name = "exit")
        val exit: Enter?,
    ) {
        @Serializable
        @JsonClass(generateAdapter = true)
        data class Enter(
            @Json(name = "addresses")
            val addresses: Addresses?,
            @Json(name = "args")
            val args: Map<String, AddressArgumentDTO>?,
        ) {
            @Serializable
            @JsonClass(generateAdapter = true)
            data class Addresses(
                @Json(name = "address")
                val address: AddressArgumentDTO?,
                @Json(name = "additionalAddresses")
                val additionalAddresses: Map<String, AddressArgumentDTO>? = null,
            )
        }
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class ValidatorDTO(
        @Json(name = "address")
        val address: String?,
        @Json(name = "status")
        val status: ValidatorStatusDTO?,
        @Json(name = "name")
        val name: String?,
        @Json(name = "image")
        val image: String?,
        @Json(name = "website")
        val website: String?,
        @Json(name = "apr")
        val apr: SerializedBigDecimal?,
        @Json(name = "commission")
        val commission: Double?,
        @Json(name = "stakedBalance")
        val stakedBalance: String?,
        @Json(name = "votingPower")
        val votingPower: Double?,
        @Json(name = "preferred")
        val preferred: Boolean?,
    ) {
        @Serializable
        @JsonClass(generateAdapter = false)
        enum class ValidatorStatusDTO {
            @SerialName("active")
            @Json(name = "active")
            ACTIVE,

            @SerialName("deactivating")
            @Json(name = "deactivating")
            DEACTIVATING,

            @SerialName("inactive")
            @Json(name = "inactive")
            INACTIVE,

            @SerialName("jailed")
            @Json(name = "jailed")
            JAILED,

            @SerialName("full")
            @Json(name = "full")
            FULL,

            UNKNOWN,
        }
    }

    @Serializable
    @JsonClass(generateAdapter = true)
    data class MetadataDTO(
        @Json(name = "name")
        val name: String?,
        @SerialName("logoURI")
        @Json(name = "logoURI")
        val logoUri: String?,
        @Json(name = "description")
        val description: String?,
        @Json(name = "documentation")
        val documentation: String?,
        @SerialName("gasFeeToken")
        @Json(name = "gasFeeToken")
        val gasFeeTokenDTO: TokenDTO?,
        @SerialName("token")
        @Json(name = "token")
        val tokenDTO: TokenDTO?,
        @SerialName("tokens")
        @Json(name = "tokens")
        val tokensDTO: List<TokenDTO>?,
        @Json(name = "type")
        val type: String?,
        @Json(name = "rewardSchedule")
        val rewardSchedule: RewardScheduleDTO?,
        @Json(name = "cooldownPeriod")
        val cooldownPeriod: PeriodDTO?,
        @Json(name = "warmupPeriod")
        val warmupPeriod: PeriodDTO?,
        @Json(name = "rewardClaiming")
        val rewardClaiming: RewardClaimingDTO?,
        @Json(name = "defaultValidator")
        val defaultValidator: String?,
        @Json(name = "minimumStake")
        val minimumStake: Int?,
        @Json(name = "supportsMultipleValidators")
        val supportsMultipleValidators: Boolean?,
        @Json(name = "revshare")
        val revshare: EnabledDTO?,
        @Json(name = "fee")
        val fee: EnabledDTO?,
    ) {

        @Serializable
        @JsonClass(generateAdapter = true)
        data class PeriodDTO(
            @Json(name = "days")
            val days: Int?,
            @Json(name = "seconds")
            val seconds: Int?,
        )

        @Serializable
        @JsonClass(generateAdapter = true)
        data class EnabledDTO(
            @Json(name = "enabled")
            val enabled: Boolean?,
        )

        @Serializable
        @JsonClass(generateAdapter = false)
        enum class RewardScheduleDTO {
            @SerialName("block")
            @Json(name = "block")
            BLOCK,

            @SerialName("week")
            @Json(name = "week")
            WEEK,

            @SerialName("hour")
            @Json(name = "hour")
            HOUR,

            @SerialName("day")
            @Json(name = "day")
            DAY,

            @SerialName("month")
            @Json(name = "month")
            MONTH,

            @SerialName("era")
            @Json(name = "era")
            ERA,

            @SerialName("epoch")
            @Json(name = "epoch")
            EPOCH,

            UNKNOWN,
        }

        @Serializable
        @JsonClass(generateAdapter = false)
        enum class RewardClaimingDTO {
            @SerialName("auto")
            @Json(name = "auto")
            AUTO,

            @SerialName("manual")
            @Json(name = "manual")
            MANUAL,

            UNKNOWN,
        }
    }

    @Serializable
    @JsonClass(generateAdapter = false)
    enum class RewardTypeDTO {
        @SerialName("apy")
        @Json(name = "apy")
        APY, // compound rate

        @SerialName("apr")
        @Json(name = "apr")
        APR, // simple rate,

        UNKNOWN,
    }
}