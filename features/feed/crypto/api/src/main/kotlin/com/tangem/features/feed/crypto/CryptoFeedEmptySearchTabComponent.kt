package com.tangem.features.feed.crypto

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.feed.nav.FeedTabComponent

interface CryptoFeedEmptySearchTabComponent : FeedTabComponent {

    interface Factory : ComponentFactory<Unit, CryptoFeedEmptySearchTabComponent>
}