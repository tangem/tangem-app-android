package com.tangem.features.tangempay.card.details

import com.tangem.domain.models.account.CardDisplayName
import com.tangem.features.tangempay.card.view.TangemPayCardDetailsUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayCardDetailsUpdateNameTransformer(
    private val displayName: CardDisplayName,
) : Transformer<TangemPayCardDetailsUM> {

    override fun transform(prevState: TangemPayCardDetailsUM): TangemPayCardDetailsUM {
        val displayNameState = prevState.displayNameState ?: return prevState
        return prevState.copy(displayNameState = displayNameState.copySealed(displayName = displayName.value))
    }
}