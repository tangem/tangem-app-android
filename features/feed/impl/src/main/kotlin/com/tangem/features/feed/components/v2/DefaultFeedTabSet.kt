package com.tangem.features.feed.components.v2

import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabSet
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultFeedTabSet @Inject constructor(
    contributors: Set<@JvmSuppressWildcards FeedTabContributor>,
) : FeedTabSet {

    override val tabs: List<FeedTabContributor> = run {
        val byId = contributors.associateBy { it.id }
        contributors
            .filter { it.id !in FeedTabsOrder }
            .forEach { TangemLogger.e("Feed tab '${it.id.value}' is not in FeedTabsOrder — skipped") }

        FeedTabsOrder.mapNotNull(byId::get).filter { it.isAvailable }
    }
}