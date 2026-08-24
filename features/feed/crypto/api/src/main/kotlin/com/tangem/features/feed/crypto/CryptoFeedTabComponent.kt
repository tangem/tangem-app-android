package com.tangem.features.feed.crypto

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.feed.nav.FeedTabComponent

/** Crypto tab of the feed home: the Market Pulse block. */
interface CryptoFeedTabComponent : FeedTabComponent {

    interface Factory : ComponentFactory<Unit, CryptoFeedTabComponent>
}