package com.tangem.features.feed.rwa.model.search

import com.tangem.features.feed.rwa.ui.state.RwaSearchListUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchPortfolioUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchUM

internal fun initialRwaSearchState(): RwaSearchUM = RwaSearchUM(
    query = "",
    portfolio = RwaSearchPortfolioUM.Empty,
    list = RwaSearchListUM.Loading,
)

/**
 * Keeps the previous results on screen while the new query loads, so refining a query does not flash a
 * shimmer over results that are about to be replaced.
 */
internal fun RwaSearchUM.startingNewQuery(query: String): RwaSearchUM = copy(
    query = query,
    list = list as? RwaSearchListUM.Content ?: RwaSearchListUM.Loading,
)

/**
 * A batch flow reports an empty list between a reload and its first batch, which reads as
 * [RwaSearchListUM.Loading]. Applying that verbatim would shimmer over results the user is still reading,
 * so a loading snapshot never displaces content already on screen — but it does replace a
 * [RwaSearchListUM.NotFound] or an [RwaSearchListUM.Error], which is how a retry gets its shimmer back.
 */
internal fun RwaSearchUM.applyListSnapshot(snapshot: RwaSearchListUM): RwaSearchUM =
    if (snapshot is RwaSearchListUM.Loading && list is RwaSearchListUM.Content) {
        this
    } else {
        copy(list = snapshot)
    }