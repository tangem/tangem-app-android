package com.tangem.features.polymarket.impl.placeprediction

/**
 * `childSlot` configuration for the Place-prediction bottom sheet hosted by
 * [com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent].
 *
 * `serializer = null` is used for the slot, so no `@Serializable` is required.
 *
 * @property eventId event the prediction is placed for
 * @property marketId optional preselected market inside the event
 * @property side optional preselected outcome side (asset id)
 */
internal data class PlacePredictionConfig(
    val eventId: String,
    val marketId: String? = null,
    val side: String? = null,
)