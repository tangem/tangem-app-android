package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

/**
 * Protocols response
 *
 * @property protocols List of protocols that are available for routing in the 1inch Aggregation protocol
 */
data class ProtocolsResponse(
    @Json(name = "protocols") val protocols: List<ProtocolImageDto>,
)

/**
 * Protocol image
 *
 * @property id Protocol id
 * @property title Protocol title
 * @property image Protocol logo image
 * @property imageColor Protocol logo image in color
 */
data class ProtocolImageDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "img") val image: String,
    @Json(name = "img_color") val imageColor: String,
)
