package com.tangem.features.tangempay

import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState

internal fun tangemPayCard(
    id: String = "card_1",
    lastDigits: String = "1234",
    frozenState: TangemPayCardFrozenState = TangemPayCardFrozenState.Unfrozen,
    state: TangemPayCardState = TangemPayCardState.Active,
): TangemPayCard = TangemPayCard(
    id = id,
    productInstanceId = "product_1",
    cardStatus = TangemPayCard.Status.ACTIVE,
    hasPinCode = true,
    displayName = null,
    limit = null,
    frozenState = frozenState,
    lastDigits = lastDigits,
    state = state,
)