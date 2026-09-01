package com.tangem.features.feed.earn.model.state.transformers

import com.tangem.features.feed.earn.ui.state.EarnListUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.utils.transformer.Transformer

internal class UpdateOpportunitiesStateLoadingTransformer : Transformer<EarnFeedTabUM> {

    override fun transform(prevState: EarnFeedTabUM): EarnFeedTabUM {
        return prevState.copy(mostlyUsed = EarnListUM.Loading)
    }
}