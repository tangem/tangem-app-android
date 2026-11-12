package com.tangem.features.feed.rwa.categories

import com.tangem.features.feed.nav.FeedFilterSheetRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import com.tangem.features.feed.v2.FeedFilterSheet
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
) : FeedFilterSheetRoute {

    override val tabId: FeedTabId get() = FeedTabId.RealAssets

    override val sheet: FeedFilterSheet get() = FeedFilterSheet.Category

    /** One selector per tab: a second push lands on the open one, which follows [ModelCallback.appliedSelection]. */
    override val screenIdentity: Any get() = CategorySelectorRoute::class
}