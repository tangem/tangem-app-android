package com.tangem.features.tangempay.model.transformers

import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayCardDataTransformer(
    private val card: TangemPayCard,
    private val onCardClick: () -> Unit,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val updatedCard = TangemPayDetailsBalanceBlockState.Card(
            lastDigits = card.lastDigits,
            onClick = onCardClick,
            isReissuing = card.state != TangemPayCardState.Active,
            isFrozen = card.frozenState == TangemPayCardFrozenState.Frozen,
        )
        val cardsBlockState = prevState.balanceBlockState.cardsBlockState?.copy(
            cards = persistentListOf(updatedCard),
        )
        val newBalanceBlockState = when (val bs = prevState.balanceBlockState) {
            is TangemPayDetailsBalanceBlockState.Loading -> bs.copy(cardsBlockState = cardsBlockState)
            is TangemPayDetailsBalanceBlockState.Content -> bs.copy(cardsBlockState = cardsBlockState)
            is TangemPayDetailsBalanceBlockState.Error -> bs.copy(cardsBlockState = cardsBlockState)
        }
        return prevState.copy(balanceBlockState = newBalanceBlockState)
    }
}