package com.tangem.features.hotwallet.addexistingwallet.root.routing

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

internal sealed class AddExistingWalletRoute : Route {

    @Serializable
    object Start : AddExistingWalletRoute()

    @Serializable
    object Import : AddExistingWalletRoute()
}