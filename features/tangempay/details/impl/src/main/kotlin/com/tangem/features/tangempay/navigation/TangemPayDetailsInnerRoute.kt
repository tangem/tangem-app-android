package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayDetailsInnerRoute : Route {
    @Serializable
    data object Details : TangemPayDetailsInnerRoute()

    @Serializable
    data object ChangePIN : TangemPayDetailsInnerRoute()

    @Serializable
    data object ChangePINSuccess : TangemPayDetailsInnerRoute()

    @Serializable
    data object AddToWallet : TangemPayDetailsInnerRoute()
}