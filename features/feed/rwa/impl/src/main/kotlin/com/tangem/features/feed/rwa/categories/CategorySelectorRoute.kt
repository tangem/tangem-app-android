package com.tangem.features.feed.rwa.categories

import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.rwa.categories.components.CategorySelectorComponent.ModelCallback
import kotlinx.serialization.Serializable

/**
 * The Category selector sheet, pushed to the feed stack by the Real assets tab.
 *
 * Carries the applied selection because a [FeedRoute] is pure data — no callback can travel with a
 * push — so the sheet has to be told what to open pre-checked. The result travels back through
 * [CategorySelection].
 */
@Serializable
internal class CategorySelectorRoute(
    val categorySelection: CategorySelection,
    val callback: ModelCallback,
) : FeedRoute