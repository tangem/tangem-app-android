package com.tangem.features.feed.model.earn.state.transformers

import com.tangem.features.feed.ui.earn.state.EarnUM

internal interface EarnUMTransformer {
    fun transform(prevState: EarnUM): EarnUM
}