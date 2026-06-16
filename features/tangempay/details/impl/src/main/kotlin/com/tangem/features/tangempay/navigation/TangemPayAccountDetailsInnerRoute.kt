package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.pay.TangemPayCard
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayAccountDetailsInnerRoute : Route {
    @Serializable
    data object AccountDetails : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class CardDetails(val cardId: String) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class AddToWallet(val card: TangemPayCard) : TangemPayAccountDetailsInnerRoute()
}