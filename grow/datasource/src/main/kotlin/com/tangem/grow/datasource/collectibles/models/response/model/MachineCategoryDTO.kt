package com.tangem.grow.datasource.collectibles.models.response.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MachineCategoryDTO(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("machinesQuantity")
    val machinesQuantity: Int,
)