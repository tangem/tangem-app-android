package com.tangem.features.feed.crypto.categories

import com.tangem.features.feed.crypto.categories.components.MarketCategorySelectorComponent
import com.tangem.features.feed.nav.FeedRoute
import kotlinx.serialization.Serializable

/**
 * @property callback receives the applied selection — the tab model implements it, which is how the
 * result travels back without a shared store
 */
@Serializable
internal class MarketCategorySelectorRoute(
    val selection: MarketCategorySelection,
    val callback: MarketCategorySelectorComponent.ModelCallback,
) : FeedRoute