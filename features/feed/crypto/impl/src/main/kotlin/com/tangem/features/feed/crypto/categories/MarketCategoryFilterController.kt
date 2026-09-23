package com.tangem.features.feed.crypto.categories

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Category filter applied to the Market Pulse list.
 *
 * The tab chip and the selector screen are separate entries on the feed stack with separate model
 * scopes, so neither can hold the other: this controller is the seam between them. The selector
 * publishes the applied choice, the tab renders the chip and narrows its list request from it.
 */
@Singleton
internal class MarketCategoryFilterController @Inject constructor() {

    val selection: StateFlow<MarketCategorySelection>
        field = MutableStateFlow(MarketCategorySelection())

    fun onSelectionApplied(selection: MarketCategorySelection) {
        this.selection.value = selection
    }

    /** Dropped with the tab, so a reopened feed starts unfiltered. */
    fun clear() {
        selection.value = MarketCategorySelection()
    }
}