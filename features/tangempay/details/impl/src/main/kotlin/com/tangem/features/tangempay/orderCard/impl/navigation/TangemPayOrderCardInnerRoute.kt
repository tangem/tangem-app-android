package com.tangem.features.tangempay.orderCard.impl.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.features.tangempay.orderCard.api.TangemPayOrderCardIntent
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayOrderCardInnerRoute : Route {

    @Serializable
    data object Type : TangemPayOrderCardInnerRoute()

    @Serializable
    data class Data(
        val deliveryEtaMaxBusinessDays: Int,
        val intent: TangemPayOrderCardIntent,
    ) : TangemPayOrderCardInnerRoute()

    @Serializable
    data class Success(
        val deliveryEtaMaxBusinessDays: Int,
        val email: String,
        val intent: TangemPayOrderCardIntent = TangemPayOrderCardIntent.Issue,
    ) : TangemPayOrderCardInnerRoute()
}