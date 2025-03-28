package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal object SendFeeLoadingTransformer : Transformer<FeeUM> {
    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState
        return state.copy(
            feeSelectorUM = state.feeSelectorUM as? FeeSelectorUM.Content ?: FeeSelectorUM.Loading,
            notifications = persistentListOf(),
            isPrimaryButtonEnabled = false,
        )
    }
}