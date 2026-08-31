package com.tangem.grow.datasource.collectibles.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.grow.datasource.collectibles.models.response.model.MachineCategoryDTO

/**
 * Response for GET /api/v1/machine/categories
 */
@JsonClass(generateAdapter = true)
data class MachineCategoriesResponse(
    @Json(name = "categories")
    val categories: List<MachineCategoryDTO>,
    @Json(name = "totalMachinesQuantity")
    val totalMachinesQuantity: Int,
)