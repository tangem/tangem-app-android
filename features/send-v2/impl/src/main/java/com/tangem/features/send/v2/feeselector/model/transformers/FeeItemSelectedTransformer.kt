package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.features.send.v2.feeselector.entity.FeeItem
import com.tangem.features.send.v2.feeselector.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal class FeeItemSelectedTransformer(private val selectedFeeItem: FeeItem) : Transformer<FeeSelectorUM> {
    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        prevState as? FeeSelectorUM.Content ?: return prevState

        return prevState.copy(selectedFeeItem = selectedFeeItem)
    }
}