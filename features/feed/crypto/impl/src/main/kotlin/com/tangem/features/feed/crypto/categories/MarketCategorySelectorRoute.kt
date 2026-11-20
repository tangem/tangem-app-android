package com.tangem.features.feed.crypto.categories

import com.tangem.features.feed.nav.FeedFilterSheetRoute
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedFilterSheet
import kotlinx.serialization.Serializable

@Serializable
internal data object MarketCategorySelectorRoute : FeedFilterSheetRoute {

    override val tabId: FeedTabId get() = FeedTabId.Crypto

    override val sheet: FeedFilterSheet get() = FeedFilterSheet.Category
}