package com.tangem.features.tangempay.verified

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayVerifiedRoute : Route {

    @Serializable
    data object Verified : TangemPayVerifiedRoute()

    @Serializable
    data object SelectPlan : TangemPayVerifiedRoute()
}