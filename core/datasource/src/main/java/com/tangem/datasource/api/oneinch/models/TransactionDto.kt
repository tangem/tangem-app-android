package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Transaction dto
 *
 * @property fromAddress transactions will be sent from this address
 * @property toAddress transactions will be sent to our(1inch) contract address
 * @property data The encoded data to call the approve method on the swapped token contract
 * @property value Native token value in WEI (for approve is always 0)
 * @property gasPrice maximum amount of gas for a swap default: 11500000; max: 11500000
 * @property gas estimated amount of the gas limit, increase this value by 25%
 * @constructor Create empty Transaction dto
 */
data class TransactionDto(
    @Json(name = "from") val fromAddress: String,
    @Json(name = "to") val toAddress: String,
    @Json(name = "data") val data: String,
    @Json(name = "value") val value: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "gas") val gas: String,
)
