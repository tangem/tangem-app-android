package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.pay.TangemPayCard
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayCardDetailsInnerRoute : Route {

    @Serializable
    data object Details : TangemPayCardDetailsInnerRoute()

    @Serializable
    data object ChangePIN : TangemPayCardDetailsInnerRoute()

    @Serializable
    data object ChangePINSuccess : TangemPayCardDetailsInnerRoute()

    @Serializable
    data class AddToWallet(val card: TangemPayCard) : TangemPayCardDetailsInnerRoute()

    @Serializable
    data class EditCardDisplayName(val card: TangemPayCard) : TangemPayCardDetailsInnerRoute()

    @Serializable
    data class LimitSetup(val card: TangemPayCard) : TangemPayCardDetailsInnerRoute()

    @Serializable
    data object LimitSetupSuccess : TangemPayCardDetailsInnerRoute()
}