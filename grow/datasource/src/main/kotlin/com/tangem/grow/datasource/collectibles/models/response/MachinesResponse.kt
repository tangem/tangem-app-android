package com.tangem.grow.datasource.collectibles.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.grow.datasource.collectibles.models.response.model.MachineDTO

/**
 * Response for GET /api/v1/machine/machines
 */
@JsonClass(generateAdapter = true)
data class MachinesResponse(
    @Json(name = "machines")
    val machines: List<MachineDTO>,
    @Json(name = "hasNext")
    val hasNext: Boolean,
    @Json(name = "cursor")
    val cursor: String?,
)