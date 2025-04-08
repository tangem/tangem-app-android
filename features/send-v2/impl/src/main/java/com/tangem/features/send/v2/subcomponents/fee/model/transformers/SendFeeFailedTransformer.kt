package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer

internal class SendFeeFailedTransformer(
    private val error: GetFeeError,
) : Transformer<FeeUM> {

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState

        return state.copy(
            feeSelectorUM = FeeSelectorUM.Error(error),
            isPrimaryButtonEnabled = false,
        )
    }
}