package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayAccountDetailsInnerRoute : Route {
    @Serializable
    data object AccountDetails : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data object CardDetails : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data object AddToWallet : TangemPayAccountDetailsInnerRoute()
}