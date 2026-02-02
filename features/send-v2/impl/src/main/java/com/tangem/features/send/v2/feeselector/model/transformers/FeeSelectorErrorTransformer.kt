package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal class FeeSelectorErrorTransformer(private val error: GetFeeError) : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        if (prevState is FeeSelectorUM.Content && error is GetFeeError.GaslessError.NotEnoughFunds) {
            return prevState.copy(
                isPrimaryButtonEnabled = false,
                feeExtraInfo = prevState.feeExtraInfo.copy(isNotEnoughFunds = true),
            )
        }

        return FeeSelectorUM.Error(error = error)
    }
}