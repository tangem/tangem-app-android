package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MultipleYieldBalancesRequestBody(
    @Json(name = "addresses") val addresses: Address,
    @Json(name = "args") val args: MultipleYieldBalancesRequestArgs,
    @Json(name = "integrationId") val integrationId: String,
) {

    @JsonClass(generateAdapter = true)
    data class Address(
        @Json(name = "address") val address: String,
        @Json(name = "additionalAddresses") val additionalAddresses: AdditionalAddresses? = null,
        @Json(name = "explorerUrl") val explorerUrl: String,
    ) {

        @JsonClass(generateAdapter = true)
        data class AdditionalAddresses(
            @Json(name = "cosmosPubKey") val cosmosPubKey: String? = null,
            @Json(name = "binanceBeaconAddress") val binanceBeaconAddress: String? = null,
            @Json(name = "stakeAccounts") val stakeAccounts: List<String>? = null,
            @Json(name = "lidoStakeAccounts") val lidoStakeAccounts: List<String>? = null,
            @Json(name = "tezosPubKey") val tezosPubKey: String? = null,
            @Json(name = "cAddressBech") val cAddressBech: String? = null,
            @Json(name = "pAddressBech") val pAddressBech: String? = null,
        )
    }

    @JsonClass(generateAdapter = true)
    data class MultipleYieldBalancesRequestArgs(
        @Json(name = "validatorAddresses") val validatorAddresses: List<String>,
    )
}