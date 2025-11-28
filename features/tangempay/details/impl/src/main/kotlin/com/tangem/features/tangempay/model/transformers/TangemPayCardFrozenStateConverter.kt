package com.tangem.features.tangempay.model.transformers

import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.entity.CardFrozenState
import com.tangem.utils.converter.Converter

internal class TangemPayCardFrozenStateConverter(
    private val onUnfreezeClick: () -> Unit,
) : Converter<TangemPayCardFrozenState, CardFrozenState> {

    override fun convert(value: TangemPayCardFrozenState): CardFrozenState {
        return when (value) {
            TangemPayCardFrozenState.Unfrozen -> CardFrozenState.Unfrozen
            TangemPayCardFrozenState.Frozen -> CardFrozenState.Frozen(onUnfreezeClick)
            TangemPayCardFrozenState.Pending -> CardFrozenState.Pending
        }
    }
}