package com.tangem.grow.datasource.collectibles.models.response

import com.tangem.grow.datasource.collectibles.models.response.model.MachineNftDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/machine/{externalId}/nfts
 */
@Serializable
data class MachineNftsResponse(
    @SerialName("nfts")
    val nfts: List<MachineNftDTO>,
    @SerialName("hasMore")
    val hasMore: Boolean,
    @SerialName("page")
    val page: Int,
    @SerialName("limit")
    val limit: Int,
)