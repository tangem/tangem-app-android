package com.tangem.features.yield.supply.api.entry

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Route for switching yield supply promo and active flows.
 */
sealed class YieldSupplyEntryRoute : Route {

    /** Initial empty route with no UI */
    data object Empty : YieldSupplyEntryRoute()

    /** Route to yield supply promo screen */
    data class Promo(
        val cryptoCurrency: CryptoCurrency,
        val apy: String,
    ) : YieldSupplyEntryRoute()

    /** Route to yield supply active screen */
    data class Active(
        val cryptoCurrency: CryptoCurrency,
    ) : YieldSupplyEntryRoute()
}