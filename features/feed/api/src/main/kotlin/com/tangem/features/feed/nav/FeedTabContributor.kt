package com.tangem.features.feed.nav

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference
import kotlinx.coroutines.flow.StateFlow

/** Identifier of a feed tab. Must match the backend's tab id once remote ordering arrives. */
@JvmInline
value class FeedTabId(val value: String) {

    companion object {
        val Crypto = FeedTabId("crypto")
        val RealAssets = FeedTabId("real_assets")
        val Earn = FeedTabId("earn")
        val Predictions = FeedTabId("predictions")
    }
}

/**
 * One tab of the feed home. Features contribute implementations into the feed's tab set with
 * `@Binds/@Provides @IntoSet`; the host builds the tab row and the pager from the set.
 *
 * Ordering is NOT declared here: the host owns a single ordered id list (`FeedTabsOrder` in
 * `:features:feed:impl`) — a contributed tab whose [id] is missing from it is skipped with an
 * error log.
 */
interface FeedTabContributor {

    val id: FeedTabId

    val title: TextReference

    /**
     * Read once, synchronously, for the initial page set — repo feature toggles are sync vals.
     * Async/remote availability is applied later through the host's pages navigation.
     */
    val isAvailable: Boolean

    /**
     * @param context child context of the feed's pager; its `router` pushes [FeedRoute]s to the
     * feed stack and falls through to the global router for everything else
     */
    fun createTab(context: AppComponentContext): FeedTabComponent

    /**
     * Search-mode page of this tab, or `null` when the tab cannot search.
     *
     * Implementing this method is the only declaration that a tab participates in search — there is
     * no separate `isSearchable` flag. Returning `null` keeps the tab in the search screen's tab row
     * (the row never shrinks) and makes the host render a "search unavailable here" page for it.
     *
     * Two obligations come with implementing it:
     *
     * - [query] is delivered **raw**, exactly as typed. Debouncing, trimming, a minimum length and
     *   cancelling superseded work all belong to the tab, because a network-backed source and an
     *   in-memory predicate want different policies.
     * - The tab **must not fetch anything while [query] is blank.** Every page of the search pager is
     *   constructed when the search screen opens, before the user has typed — a request fired from
     *   `init` therefore runs on every tab on every search entry, for nothing.
     *
     * @param context child context of the search screen's pager; routes behave as in [createTab]
     * @param query the search bar's current text, shared by every tab of the search screen
     */
    fun createSearchTab(context: AppComponentContext, query: StateFlow<String>): FeedTabComponent? = null
}