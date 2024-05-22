package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EnabledYieldsResponse(
    @Json(name = "data")
    val data: List<Yield>,
    @Json(name = "hasNextPage")
    val hasNextPage: Boolean,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "page")
    val page: Int,
)

@JsonClass(generateAdapter = true)
data class Enter(
    @Json(name = "addresses")
    val addresses: Addresses,
    @Json(name = "args")
    val args: Map<String, AddressArgument>,
)

@JsonClass(generateAdapter = true)
data class Addresses(
    @Json(name = "address")
    val address: AddressArgument,
    @Json(name = "additionalAddresses")
    val additionalAddresses: Map<String, AddressArgument>? = null,
)

@JsonClass(generateAdapter = true)
data class AddressArgument(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "network")
    val network: String? = null,
    @Json(name = "minimum")
    val minimum: Int? = null,
    @Json(name = "maximum")
    val maximum: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Status(
    @Json(name = "enter")
    val enter: Boolean,
    @Json(name = "exit")
    val exit: Boolean?,
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
    val cooldownPeriod: CooldownPeriod,
    @Json(name = "warmupPeriod")
    val warmupPeriod: CooldownPeriod,
    @Json(name = "rewardClaiming")
    val rewardClaiming: String,
    @Json(name = "defaultValidator")
    val defaultValidator: String?,
    @Json(name = "minimumStake")
    val minimumStake: Int,
    @Json(name = "supportsMultipleValidators")
    val supportsMultipleValidators: Boolean,
    @Json(name = "revshare")
    val revshare: Enabled,
    @Json(name = "fee")
    val fee: Enabled,
)

@JsonClass(generateAdapter = true)
data class CooldownPeriod(
    @Json(name = "days")
    val days: Int,
)

@JsonClass(generateAdapter = true)
data class Enabled(
    @Json(name = "enabled")
    val enabled: Boolean,
)