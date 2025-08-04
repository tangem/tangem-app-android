package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal object FeeSelectorRemoveSuggestedTransformer : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val state = prevState as? FeeSelectorUM.Content ?: return prevState
        val newFeeItems = state.feeItems.filterNot { item -> item is FeeItem.Suggested }
        val newSelectedFee = if (state.selectedFeeItem is FeeItem.Suggested) {
            state.feeItems.first { item -> item is FeeItem.Market }
        } else {
            state.selectedFeeItem
        }

        return state.copy(
            feeItems = newFeeItems.toImmutableList(),
            selectedFeeItem = newSelectedFee,
        )
    }
}