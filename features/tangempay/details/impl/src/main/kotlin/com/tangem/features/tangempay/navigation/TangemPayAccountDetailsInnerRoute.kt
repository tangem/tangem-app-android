package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.features.tangempay.tiers.select.TangemPaySelectPlanSource
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayAccountDetailsInnerRoute : Route {
    @Serializable
    data object AccountDetails : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class CardDetails(val cardId: String) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class AddToWallet(val card: TangemPayCard) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class CurrentPlan(
        val tariffPlan: TangemPayCustomerTariffPlan,
    ) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data class SelectPlan(
        val tariffPlan: TangemPayCustomerTariffPlan,
        val source: TangemPaySelectPlanSource,
    ) : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data object VirtualAccountDepositSuccess : TangemPayAccountDetailsInnerRoute()

    @Serializable
    data object Cashback : TangemPayAccountDetailsInnerRoute()
}