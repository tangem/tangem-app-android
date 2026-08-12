package com.tangem.features.feed.nav

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.TextReference

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
}