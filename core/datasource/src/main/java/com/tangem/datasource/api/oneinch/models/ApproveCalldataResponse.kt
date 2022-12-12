package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Approve calldata response
 *
 * @property data The encoded data to call the approve method on the swapped token contract
 * @property gasPrice Gas price for fast transaction processing
 * @property toAddress Token address that will be allowed to exchange through 1inch router
 * @property value Native token value in WEI (for approve is always 0)
 */
data class ApproveCalldataResponse(
    @Json(name = "data") val data: String,
    @Json(name = "gasPrice") val gasPrice: String,
    @Json(name = "to") val toAddress: String,
    @Json(name = "value") val value: String,
)
