package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class Yield(
    @Json(name = "id")
    val id: String,
    @Json(name = "token")
    val token: Token,
    @Json(name = "tokens")
    val tokens: List<Token>,
    @Json(name = "args")
    val args: Args,
    @Json(name = "status")
    val status: Status,
    @Json(name = "apy")
    val apy: BigDecimal,
    @Json(name = "rewardRate")
    val rewardRate: Double,
    @Json(name = "rewardType")
    val rewardType: RewardType,
    @Json(name = "metadata")
    val metadata: Metadata,
    @Json(name = "validators")
    val validators: List<Validator>,
    @Json(name = "isAvailable")
    val isAvailable: Boolean,
) {

    @JsonClass(generateAdapter = true)
    data class Status(
        @Json(name = "enter")
        val enter: Boolean,
        @Json(name = "exit")
        val exit: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class Args(
        @Json(name = "enter")
        val enter: Enter,
        @Json(name = "exit")
        val exit: Enter?,
    ) {
        @JsonClass(generateAdapter = true)
        data class Enter(
            @Json(name = "addresses")
            val addresses: Addresses,
            @Json(name = "args")
            val args: Map<String, AddressArgument>,
        ) {
            @JsonClass(generateAdapter = true)
            data class Addresses(
                @Json(name = "address")
                val address: AddressArgument,
                @Json(name = "additionalAddresses")
                val additionalAddresses: Map<String, AddressArgument>? = null,
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class Validator(
        @Json(name = "address")
        val address: String,
        @Json(name = "status")
        val status: String,
        @Json(name = "name")
        val name: String,
        @Json(name = "image")
        val image: String?,
        @Json(name = "website")
        val website: String?,
        @Json(name = "apr")
        val apr: Double?,
        @Json(name = "commission")
        val commission: Double?,
        @Json(name = "stakedBalance")
        val stakedBalance: String?,
        @Json(name = "votingPower")
        val votingPower: Double?,
        @Json(name = "preferred")
        val preferred: Boolean,
    )

    @JsonClass(generateAdapter = true)
    data class Metadata(
        @Json(name = "name")
        val name: String,
        @Json(name = "logoURI")
        val logoUri: String,
        @Json(name = "description")
        val description: String,
        @Json(name = "documentation")
        val documentation: String?,
        @Json(name = "gasFeeToken")
        val gasFeeToken: Token,
        @Json(name = "token")
        val token: Token,
        @Json(name = "tokens")
        val tokens: List<Token>,
        @Json(name = "type")
        val type: String,
        @Json(name = "rewardSchedule")
        val rewardSchedule: String,
        @Json(name = "cooldownPeriod")
        val cooldownPeriod: Period,
        @Json(name = "warmupPeriod")
        val warmupPeriod: Period,
        @Json(name = "rewardClaiming")
        val rewardClaiming: String,
        @Json(name = "defaultValidator")
        val defaultValidator: String?,
        @Json(name = "minimumStake")
        val minimumStake: Int?,
        @Json(name = "supportsMultipleValidators")
        val supportsMultipleValidators: Boolean,
        @Json(name = "revshare")
        val revshare: Enabled,
        @Json(name = "fee")
        val fee: Enabled,
    ) {

        @JsonClass(generateAdapter = true)
        data class Period(
            @Json(name = "days")
            val days: Int,
        )

        @JsonClass(generateAdapter = true)
        data class Enabled(
            @Json(name = "enabled")
            val enabled: Boolean,
        )
    }

    enum class RewardType {
        @Json(name = "apy")
        APY, // auto
        @Json(name = "apr")
        APR, // manual
    }
}