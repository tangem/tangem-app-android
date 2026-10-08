package com.tangem.grow.datasource.collectibles.models.response

import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/machine/machines
 */
@Serializable
data class MachinesResponse(
    @SerialName("machines")
    val machines: List<MachineDTO>,
    @SerialName("hasNext")
    val hasNext: Boolean,
    @SerialName("cursor")
    val cursor: String?,
)