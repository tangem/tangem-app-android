package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.TangemPayActionButtonUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class TangemPayActionButtonsTransformer(
    private val actionButtons: ImmutableList<TangemPayActionButtonUM>,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val balanceBlockState = prevState.balanceBlockState
        if (balanceBlockState !is TangemPayDetailsBalanceBlockState.Content) return prevState
        return prevState.copy(balanceBlockState = balanceBlockState.copy(actionButtons = actionButtons))
    }
}