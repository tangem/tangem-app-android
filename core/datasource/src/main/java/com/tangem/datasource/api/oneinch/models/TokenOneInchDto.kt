package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Token one inch
 *
 * @property symbol token symbol
 * @property name token name
 * @property address token address
 * @property decimals token decimals
 * @property logoURI token logo image url
 */
data class TokenOneInchDto(
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "logoURI") val logoURI: String,
)
