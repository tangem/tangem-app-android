package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class UpdateEarnFiltersTransformer(
    private val filters: ImmutableList<TangemFilterItemUM>,
) : Transformer<EarnFeedTabUM> {

    override fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM = prevState.copy(filters = filters)
}