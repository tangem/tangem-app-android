package com.tangem.features.tangempay.navigation

import com.tangem.core.decompose.navigation.Route
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
    data object AddToWallet : TangemPayCardDetailsInnerRoute()

    @Serializable
    data class EditCardDisplayName(val cardId: String) : TangemPayCardDetailsInnerRoute()

    @Serializable
    data object LimitSetup : TangemPayCardDetailsInnerRoute()

    @Serializable
    data object LimitSetupSuccess : TangemPayCardDetailsInnerRoute()
}