package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.pay.TangemPayDetailsConfig
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayAccountDetailsInnerRoute : Route {
    @Serializable
    data object AccountDetails : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class CardDetails(val config: TangemPayDetailsConfig) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data object AddToWallet : TangemPayAccountDetailsInnerRoute()
}