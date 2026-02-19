package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.earn.state.EarnUM

internal class UpdateMostlyUsedStateLoadingTransformer : EarnUMTransformer {

    override fun transform(prevState: EarnUM): EarnUM {
        return prevState.copy(mostlyUsed = EarnListUM.Loading)
    }
}