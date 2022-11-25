package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Swap error dto
 *
 * One of the following errors:
 *
 * -Insufficient liquidity
 * -Cannot estimate
 * -You may not have enough ETH balance for gas fee
 * -FromTokenAddress cannot be equals to toTokenAddress
 * -Cannot estimate. Don't forget about miner fee. Try to leave the buffer of ETH for gas
 * -Not enough balance
 * -Not enough allowance
 *
 * @property statusCode HTTP code
 * @property error Error code description
 * @property description Error description (one of the following)
 * @property requestId Request id
 * @property meta Meta information
 * @constructor Create empty Swap error dto
 */
data class SwapErrorDto(
    @Json(name = "statusCode") val statusCode: Int,
    @Json(name = "error") val error: String,
    @Json(name = "description") val description: String,
    @Json(name = "requestId") val requestId: String,
    @Json(name = "meta") val meta: List<NestErrorMeta>,
)

/**
 * Nest error meta
 *
 * @property type Type of field
 * @property value Value of field
 */
data class NestErrorMeta(
    @Json(name = "type") val type: String,
    @Json(name = "value") val value: String,
)
