package com.tangem.grow.datasource.collectibles.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MachineCategoryDTO(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "machinesQuantity")
    val machinesQuantity: Int,
)