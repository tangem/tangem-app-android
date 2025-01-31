package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

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
    val apy: BigDecimal?,
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

    @JsonClass(generateAdapter = true)
    data class StatusDTO(
        @Json(name = "enter")
        val enter: Boolean?,
        @Json(name = "exit")
        val exit: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class ArgsDTO(
        @Json(name = "enter")
        val enter: Enter?,
        @Json(name = "exit")
        val exit: Enter?,
    ) {
        @JsonClass(generateAdapter = true)
        data class Enter(
            @Json(name = "addresses")
            val addresses: Addresses?,
            @Json(name = "args")
            val args: Map<String, AddressArgumentDTO>?,
        ) {
            @JsonClass(generateAdapter = true)
            data class Addresses(
                @Json(name = "address")
                val address: AddressArgumentDTO?,
                @Json(name = "additionalAddresses")
                val additionalAddresses: Map<String, AddressArgumentDTO>? = null,
            )
        }
    }

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
        val apr: BigDecimal?,
        @Json(name = "commission")
        val commission: Double?,
        @Json(name = "stakedBalance")
        val stakedBalance: String?,
        @Json(name = "votingPower")
        val votingPower: Double?,
        @Json(name = "preferred")
        val preferred: Boolean?,
    ) {
        @JsonClass(generateAdapter = false)
        enum class ValidatorStatusDTO {
            @Json(name = "active")
            ACTIVE,

            @Json(name = "deactivating")
            DEACTIVATING,

            @Json(name = "inactive")
            INACTIVE,

            @Json(name = "jailed")
            JAILED,

            @Json(name = "full")
            FULL,

            UNKNOWN,
        }
    }

    @JsonClass(generateAdapter = true)
    data class MetadataDTO(
        @Json(name = "name")
        val name: String?,
        @Json(name = "logoURI")
        val logoUri: String?,
        @Json(name = "description")
        val description: String?,
        @Json(name = "documentation")
        val documentation: String?,
        @Json(name = "gasFeeToken")
        val gasFeeTokenDTO: TokenDTO?,
        @Json(name = "token")
        val tokenDTO: TokenDTO?,
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

        @JsonClass(generateAdapter = true)
        data class PeriodDTO(
            @Json(name = "days")
            val days: Int?,
        )

        @JsonClass(generateAdapter = true)
        data class EnabledDTO(
            @Json(name = "enabled")
            val enabled: Boolean?,
        )

        @JsonClass(generateAdapter = false)
        enum class RewardScheduleDTO {
            @Json(name = "block")
            BLOCK,

            @Json(name = "week")
            WEEK,

            @Json(name = "hour")
            HOUR,

            @Json(name = "day")
            DAY,

            @Json(name = "month")
            MONTH,

            @Json(name = "era")
            ERA,

            @Json(name = "epoch")
            EPOCH,

            UNKNOWN,
        }

        @JsonClass(generateAdapter = false)
        enum class RewardClaimingDTO {
            @Json(name = "auto")
            AUTO,

            @Json(name = "manual")
            MANUAL,

            UNKNOWN,
        }
    }

    @JsonClass(generateAdapter = false)
    enum class RewardTypeDTO {
        @Json(name = "apy")
        APY, // compound rate
        @Json(name = "apr")
        APR, // simple rate,

        UNKNOWN,
    }
}