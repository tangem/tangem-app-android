package com.tangem.features.polymarket.impl.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Internal navigation routes for the Polymarket feature stack.
 *
 * Used as the `childStack` configuration inside [com.tangem.features.polymarket.impl.DefaultPolymarketComponent].
 * `serializer = null` is used in the stack, so no `@Serializable` is required here.
 */
internal sealed interface PolymarketRoute : Route {

    /** The prerequisite for the gate: a known wallet. */
    data object Entry : PolymarketRoute

    /**
     * Entry gate for [userWalletId] — the wallet [Entry] settled, which may differ from the one the feature was
     * opened with.
     */
    data class Onboarding(val userWalletId: UserWalletId) : PolymarketRoute

    /**
     * Discovery feed — reached once [Onboarding] resolves the entry decision.
     *
     * @property userWalletId the wallet the entry gate resolved, carried on the route rather than taken from
     *  the feature params because the gate may resolve a *different* wallet than the one the feature was
     *  opened with — the user can choose among several eligible wallets.
     */
    data class Main(val userWalletId: UserWalletId) : PolymarketRoute

    /**
     * Details of a single prediction event.
     *
     * @property eventId event to show
     * @property userWalletId the wallet this event belongs to, carried on the route rather than taken from
     *  the feature params for the same reason as [Main.userWalletId] — the entry gate may have resolved a
     *  different wallet than the one the feature was opened with.
     * @property marketId market preselected by the caller, e.g. by tapping an outcome on the feed card
     * @property assetId outcome preselected by the caller
     */
    data class EventDetails(
        val eventId: String,
        val userWalletId: UserWalletId,
        val marketId: String? = null,
        val assetId: String? = null,
    ) : PolymarketRoute

    /**
     * Full-text search over discoverable events.
     *
     * @property userWalletId the wallet the feature runs for, carried for the same reason as
     *  [Main.userWalletId] — a tapped result opens [EventDetails], which needs it
     */
    data class Search(val userWalletId: UserWalletId) : PolymarketRoute
}