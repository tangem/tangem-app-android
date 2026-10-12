package com.tangem.grow.datasource.collectibles.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Problem details body of a collectibles backend error; the gacha's failures arrive mapped to its own [code].
 *
 * @property code absent on validation and unexpected server errors
 * @property detail human-readable, for logs only
 */
@Serializable
data class CollectiblesErrorResponse(
    @SerialName("code")
    val code: Int?,
    @SerialName("detail")
    val detail: String?,
)