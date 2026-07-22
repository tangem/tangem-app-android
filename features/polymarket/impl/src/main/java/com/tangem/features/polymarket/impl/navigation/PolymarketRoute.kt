package com.tangem.features.polymarket.impl.navigation

import com.tangem.core.decompose.navigation.Route

/**
 * Internal navigation routes for the Polymarket feature stack.
 *
 * Used as the `childStack` configuration inside [com.tangem.features.polymarket.impl.DefaultPolymarketComponent].
 * `serializer = null` is used in the stack, so no `@Serializable` is required here.
 */
internal sealed interface PolymarketRoute : Route {

    /** Discovery feed — the entry screen of the feature. */
    data object Main : PolymarketRoute

    /**
     * Details of a single prediction event.
     *
     * @property eventId event to show
     * @property marketId market preselected by the caller, e.g. by tapping an outcome on the feed card
     * @property assetId outcome preselected by the caller
     */
    data class EventDetails(
        val eventId: String,
        val marketId: String? = null,
        val assetId: String? = null,
    ) : PolymarketRoute

    /** Events/markets search screen. */
    data object Search : PolymarketRoute
}