package com.tangem.features.feed.crypto

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.feed.nav.FeedTabComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * Crypto tab of the feed's search screen: the user's matching holdings followed by matching market
 * tokens.
 *
 * It is a separate component from [CryptoFeedTabComponent] rather than a mode of it: the two share
 * only the market row, and a single component would build the feed's Market Pulse list on the search
 * screen and vice versa.
 */
interface CryptoFeedSearchTabComponent : FeedTabComponent {

    /**
     * @property query the search bar's text, raw. The tab owns debouncing and trimming, and must not
     * fetch while it is blank — see `FeedTabContributor.createSearchTab`.
     */
    data class Params(val query: StateFlow<String>)

    interface Factory : ComponentFactory<Params, CryptoFeedSearchTabComponent>
}