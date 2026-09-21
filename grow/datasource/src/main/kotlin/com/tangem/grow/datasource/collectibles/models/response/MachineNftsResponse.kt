package com.tangem.grow.datasource.collectibles.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.grow.datasource.collectibles.models.response.model.MachineNftDTO

/**
 * Response for GET /api/v1/machine/{externalId}/nfts
 */
@JsonClass(generateAdapter = true)
data class MachineNftsResponse(
    @Json(name = "nfts")
    val nfts: List<MachineNftDTO>,
    @Json(name = "hasMore")
    val hasMore: Boolean,
    @Json(name = "page")
    val page: Int,
    @Json(name = "limit")
    val limit: Int,
)