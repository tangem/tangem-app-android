package com.tangem.features.tangempay.orderCard.impl.navigation

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayOrderCardInnerRoute : Route {

    @Serializable
    data object Type : TangemPayOrderCardInnerRoute()
}