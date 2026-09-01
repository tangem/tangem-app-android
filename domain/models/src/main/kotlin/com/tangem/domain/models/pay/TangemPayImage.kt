package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Artwork the backend attaches to a card, a tariff plan or an offer.
 *
 * [type] stays the raw backend value: each endpoint has its own set of types, so the owner of the
 * images exposes named accessors for the ones it actually serves instead of sharing one vocabulary.
 */
@Serializable
data class TangemPayImage(
    @SerialName("type") val type: String,
    @SerialName("url") val url: String,
) {

    fun hasType(wireType: String): Boolean = type.equals(wireType, ignoreCase = true)
}

internal fun List<TangemPayImage>.urlOfType(wireType: String): String? = firstOrNull { it.hasType(wireType) }?.url