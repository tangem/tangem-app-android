package com.tangem.features.tangempay.model.transformers

import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class TangemPayFreezeUnfreezeStateTransformer(
    private val cardFrozenState: TangemPayCardFrozenState,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val balanceBlockState = if (prevState.balanceBlockState is TangemPayDetailsBalanceBlockState.Content) {
            val actionButtons = prevState.balanceBlockState.actionButtons.map {
                it.copy(isEnabled = cardFrozenState == TangemPayCardFrozenState.Unfrozen)
            }
            prevState.balanceBlockState.copy(actionButtons = actionButtons.toPersistentList())
        } else {
            prevState.balanceBlockState
        }
        return prevState.copy(balanceBlockState = balanceBlockState)
    }
}