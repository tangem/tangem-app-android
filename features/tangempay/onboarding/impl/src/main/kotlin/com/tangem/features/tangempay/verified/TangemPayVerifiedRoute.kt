package com.tangem.features.tangempay.verified

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.pay.TangemPayProduct
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayVerifiedRoute : Route {

    @Serializable
    data object Verified : TangemPayVerifiedRoute()

    @Serializable
    data class SelectPlan(
        @SerialName("availableProducts") val availableProducts: Set<TangemPayProduct>,
    ) : TangemPayVerifiedRoute()
}