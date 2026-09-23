package com.tangem.features.feed.crypto

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.feed.nav.FeedEmptySearchComponent

interface CryptoFeedEmptySearchComponent : FeedEmptySearchComponent {

    interface Factory : ComponentFactory<Unit, CryptoFeedEmptySearchComponent>
}