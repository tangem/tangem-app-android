package com.tangem.grow.datasource.collectibles.models.response

import com.tangem.grow.datasource.collectibles.models.response.model.MachineCategoryDTO
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for GET /api/v1/machine/categories
 */
@Serializable
data class MachineCategoriesResponse(
    @SerialName("categories")
    val categories: List<MachineCategoryDTO>,
    @SerialName("totalMachinesQuantity")
    val totalMachinesQuantity: Int,
)