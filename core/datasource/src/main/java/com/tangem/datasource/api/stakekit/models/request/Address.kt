package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json

data class Address(
    @Json(name = "address")
    val address: String,
    @Json(name = "additionalAddresses")
    val additionalAddresses: AdditionalAddresses? = null,
    @Json(name = "explorerUrl")
    val explorerUrl: String? = null,
) {

    data class AdditionalAddresses(
        // cosmos-specific
        @Json(name = "cosmosPubKey")
        val cosmosPubKey: String? = null,

        // binance-specific
        @Json(name = "binanceBeaconAddress")
        val binanceBeaconAddress: String? = null,

        // solana-specific
        @Deprecated("Legacy in StakeKit, isn't used in Solana")
        @Json(name = "stakeAccounts")
        val stakeAccounts: List<String>? = null,
        @Json(name = "lidoStakeAccounts")
        val lidoStakeAccounts: List<String>? = null,

        // tezos-specific
        @Json(name = "tezosPubKey")
        val tezosPubKey: String? = null,

        // avalanche-specific
        @Json(name = "cAddressBech")
        val cAddressBech: String? = null,
        @Json(name = "pAddressBech")
        val pAddressBech: String? = null,
    )
}