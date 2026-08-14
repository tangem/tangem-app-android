package com.tangem.features.feed.search

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent

/**
 * The feed's search screen: results below the host's pinned search bar. The query arrives through
 * [FeedSearchBarController], not through a search field of its own.
 */
interface FeedSearchComponent : FeedScreenComponent {

    interface Factory : ComponentFactory<FeedRoute.Search, FeedSearchComponent>
}