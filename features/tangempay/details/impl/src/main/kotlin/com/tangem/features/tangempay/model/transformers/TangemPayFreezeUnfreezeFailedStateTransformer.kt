package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.CardFrozenState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayFreezeUnfreezeFailedStateTransformer(
    isCardFrozen: Boolean,
    onUnfreezeClick: () -> Unit,
) : Transformer<TangemPayDetailsUM> {

    private val cardFrozenState =
        if (isCardFrozen) CardFrozenState.Frozen(onUnfreezeClick) else CardFrozenState.Unfrozen

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val updatedBalanceState = when (val balanceState = prevState.balanceBlockState) {
            is TangemPayDetailsBalanceBlockState.Error -> balanceState.copy(frozenState = cardFrozenState)
            is TangemPayDetailsBalanceBlockState.Loading -> balanceState.copy(frozenState = cardFrozenState)
            is TangemPayDetailsBalanceBlockState.Content -> balanceState.copy(
                isBalanceFlickering = false,
                frozenState = cardFrozenState,
            )
        }

        return prevState.copy(balanceBlockState = updatedBalanceState)
    }
}