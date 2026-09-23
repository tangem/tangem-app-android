package com.tangem.features.feed.rwa.categories

import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import kotlinx.serialization.Serializable

/**
 * The Category selector screen, pushed to the feed stack by the Real assets tab.
 *
 * [categorySelection] is what the screen opens pre-checked with; the chosen one comes back by calling
 * [callback], not through the route.
 */
@Serializable
internal class CategorySelectorRoute(
    val categorySelection: CategorySelection,
    val callback: ModelCallback,
) : FeedRoute