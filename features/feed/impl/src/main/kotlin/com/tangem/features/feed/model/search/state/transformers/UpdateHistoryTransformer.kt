package com.tangem.features.feed.model.search.state.transformers

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.features.feed.ui.search.state.SearchContentUM
import com.tangem.features.feed.ui.search.state.SearchUM
import com.tangem.features.feed.ui.search.state.TextHintItemUM
import kotlinx.collections.immutable.ImmutableList

internal class UpdateHistoryTransformer(
    private val textHints: ImmutableList<TextHintItemUM>,
    private val recentTokens: ImmutableList<MarketsListItemUM>,
) : SearchUMTransformer {

    override fun transform(prevState: SearchUM): SearchUM {
        return prevState.copy(
            content = SearchContentUM.History(
                textHints = textHints,
                recentTokens = recentTokens,
            ),
        )
    }
}