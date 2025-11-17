package com.tangem.features.tangempay.model.transformers

import com.tangem.features.tangempay.entity.CardFrozenState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal object TangemPayFreezeUnfreezeProcessTransformer : Transformer<TangemPayDetailsUM> {
    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val newBalanceBlockState = when (val balanceState = prevState.balanceBlockState) {
            is TangemPayDetailsBalanceBlockState.Error -> balanceState.copy(frozenState = CardFrozenState.Pending)
            is TangemPayDetailsBalanceBlockState.Loading -> balanceState.copy(frozenState = CardFrozenState.Pending)
            is TangemPayDetailsBalanceBlockState.Content -> balanceState.copy(
                isBalanceFlickering = true,
                frozenState = CardFrozenState.Pending,
            )
        }

        return prevState.copy(balanceBlockState = newBalanceBlockState)
    }
}